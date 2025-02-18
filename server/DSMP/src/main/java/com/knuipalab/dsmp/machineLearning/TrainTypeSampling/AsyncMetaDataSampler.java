package com.knuipalab.dsmp.machineLearning.TrainTypeSampling;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Lists;
import com.knuipalab.dsmp.machineLearning.MachineLearningInference.MalignancyServerMessenger;
import com.knuipalab.dsmp.metadata.MetaData;
import com.knuipalab.dsmp.metadata.MetaDataRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;

@Service
@RequiredArgsConstructor
public class AsyncMetaDataSampler implements MetaDataSampler {


    private final MalignancyServerMessenger malignancyServerMessenger;

    private final MetaDataRepository metaDataRepository;

    @Override
    public void typeSampling(String projectId) {

        List<MetaData> metaDataList = metaDataRepository.findByProjectId(projectId);

        final int NUM_THREADS = Runtime.getRuntime().availableProcessors() + 1;
        ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(NUM_THREADS);

        int seq = 0;
        int chunk_size = 1000;
        List<Future<Runnable>> futures = new ArrayList<Future<Runnable>>();
        for (List<MetaData> batch : Lists.partition(metaDataList,chunk_size)) {
            Future f = executor.submit(new updateThread(batch, seq));
            futures.add(f);
            seq += 1;
        }
        // wait for all tasks to complete before continuing
        for (Future<Runnable> f : futures)
        {
            try {
                f.get();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
        }
        //shut down the executor service so that this thread can exit
        executor.shutdown();

    }
    /**
     * 프로젝트 아이디를 통해서 요청하면,
     * 해당 프로젝트에 포함된 이미지 데이터들에 대한 머신러닝 추론이 시작됩니다.
     * 수치형 데이터결과는 메타데이터에 추가되며, 이미지 데이터 결과는 .cam 확장자가
     * 중간에 삽입되어서 이미지 데이터에 저장됩니다.(ex. 123.jpg -> 123.cam.png)
     * @param projectId
     * @return
     */
    @Override
    public void requestMLInferenceToTorchServe(String projectId,String modelName) {

        List<MetaData> metaDataList = metaDataRepository.findByProjectId(projectId);

        final int NUM_THREADS = Runtime.getRuntime().availableProcessors() + 1;
        ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(NUM_THREADS);

        List<Future<Runnable>> futures = new ArrayList<Future<Runnable>>();
        for ( MetaData metadata : metaDataList ) {
            Future f = executor.submit(new getAndSetMalignancyClassificationDataThread(
                    metadata.getMetadataId(),projectId,metadata.getImageNameFromBody(),modelName));
            futures.add(f);
        }
        // wait for all tasks to complete before continuing
        for (Future<Runnable> f : futures)
        {
            try {
                f.get();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
        }
        //shut down the executor service so that this thread can exit
        executor.shutdown();
    }

    class getAndSetMalignancyClassificationDataThread implements Runnable {

        private String metadataId;
        private String projectId,imageName,modelName;

        public getAndSetMalignancyClassificationDataThread(String metadataId,String projectId,String imageName,String modelName){
            this.metadataId = metadataId;
            this.projectId=projectId;
            this.imageName = imageName;
            this.modelName=modelName;
        }

        @Override
        public void run() {
            JsonNode jsonNode = malignancyServerMessenger.requestMalignancyInference(projectId, imageName,modelName);
            if(jsonNode == null) {
                throw new NullPointerException();
            }
            HashMap<String,Object> classificationSet = new HashMap<>();
            jsonNode.fields().forEachRemaining(
                    field -> { classificationSet.put(field.getKey(),field.getValue()); }
            );
            metaDataRepository.setMalignancyClassification(metadataId,classificationSet);
        }

    }

    class updateThread implements Runnable {

        private List<MetaData> metaDataList;
        int seq;

        public updateThread(List<MetaData> metaDataList,int seq){
            this.metaDataList = metaDataList;
            this.seq = seq;
        }

        @Override
        public void run() {

            double trainPercent = 0.7 , validPercent = 0.2 , testPercent = 0.1 ;
            int metaDataListSize = metaDataList.size();
            int trainSize = (int)(metaDataListSize * trainPercent);
            int validSize = (int)(metaDataListSize * validPercent);
            int testSize = metaDataListSize - (trainSize+validSize);
            int randomValue;

            SampleType sampleType = null;
            int cnt = 0;
            while(trainSize!=0 || validSize!=0 || testSize!=0 ) {
                randomValue = (int)(Math.random() * 10);
                if( 0 <= randomValue && randomValue < 7 && trainSize!=0 ){
                    trainSize -= 1;
                    sampleType = SampleType.TRAIN;
                }
                else if ( randomValue < 9 && validSize!=0 ) {
                    validSize -= 1;
                    sampleType = SampleType.VALID;
                }
                else if ( randomValue < 10 && testSize!=0 ) {
                    testSize -= 1;
                    sampleType = SampleType.TEST;
                }
                else {
                    continue;
                }
                metaDataRepository.updateType(metaDataList.get(cnt).getMetadataId(),sampleType.getTypeString());
                cnt += 1;
            }
        }
    }

}
