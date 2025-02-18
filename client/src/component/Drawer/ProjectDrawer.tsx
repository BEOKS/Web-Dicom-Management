import './ProjectDrawer.css';
import { useTheme, List, ListItemButton, ListItemIcon, ListItemText, Divider, IconButton, Grid, Typography } from '@mui/material';
import ChevronLeftIcon from '@mui/icons-material/ChevronLeft';
import ChevronRightIcon from '@mui/icons-material/ChevronRight';
import FolderOpenIcon from '@mui/icons-material/FolderOpen';
import AddCircleIcon from '@mui/icons-material/AddCircle';
import { useDispatch, useSelector } from 'react-redux';
import { MetaDataGridAction } from '../Table/MetaDataGridReducer';
import { Project, ProjectDrawerAction } from './ProjectDrawerReducer';
import { VisualTableAction } from '../VisualTable/VisualTableReducer';
import CreateProjectDialog from './CreateProjectDialog';
import { Drawer, DrawerHeader } from './ProjectDrawerStyling';
import { RootState } from '../../store';

const CREATED = false, INVITED = true;

export default function ProjectDrawer() {
    const theme = useTheme();
    const dispatch = useDispatch();

    const openProjectDrawer = useSelector((state: RootState) => state.ProjectDrawerReducer.openProjectDrawer);
    const presentProject = useSelector((state: RootState) => state.ProjectDrawerReducer.project);
    const createdProjects = useSelector((state: RootState) => state.ProjectDrawerReducer.createdProjects);
    const invitedProjects = useSelector((state: RootState) => state.ProjectDrawerReducer.invitedProjects);

    const createListItemButton = (project: Project, type: boolean) => {
        return (
            <ListItemButton
                selected={presentProject.projectId === project.projectId}
                key={project.projectId}
                onClick={() => {
                    dispatch(MetaDataGridAction.setSelectedRow([]));
                    dispatch(MetaDataGridAction.setSelectedMetaDataID([]));
                    dispatch(MetaDataGridAction.setSelectedStudyUID([]));
                    dispatch(ProjectDrawerAction.setProject(project));
                    dispatch(ProjectDrawerAction.markInvitedProject(type));
                    dispatch(VisualTableAction.setOptions([]));
                }}>
                <ListItemIcon>
                    <FolderOpenIcon />
                </ListItemIcon>
                <ListItemText primary={project.projectName} />
            </ListItemButton>
        );
    };

    return (
        <div>
            <Drawer variant="permanent" open={openProjectDrawer}>
                <DrawerHeader>
                    <IconButton onClick={() => dispatch(ProjectDrawerAction.closeProjectDrawer())}>
                        {theme.direction === 'rtl' ? <ChevronRightIcon /> : <ChevronLeftIcon />}
                    </IconButton>
                </DrawerHeader>
                <Divider />
                {openProjectDrawer && createdProjects.length > 0 && (
                    <Typography className="category" variant="subtitle2" component="div">Created Projects</Typography>
                )}

                <List sx={{ py: 0 }}>
                    {createdProjects.map((project) => (
                        createListItemButton(project, CREATED)
                    ))}
                </List>
                {createdProjects.length > 0 && <Divider />}
                {openProjectDrawer && invitedProjects.length > 0 && (
                    <Typography className="category" variant="subtitle2" component="div">Invited Projects</Typography>
                )}
                <List sx={{ py: 0 }}>
                    {invitedProjects.map((project) => (
                        createListItemButton(project, INVITED)
                    ))}
                </List>
                {invitedProjects.length > 0 && <Divider />}
                <Grid
                    container
                    height='100%'
                    alignItems='flex-end'
                >
                    <List
                        sx={{
                            width: '100%',
                            color: 'primary.main',
                            fontWeight: 'bold'
                        }}
                    >
                        <ListItemButton key='Add Project' onClick={() => dispatch(ProjectDrawerAction.openCreateProjectDialog())}>
                            <ListItemIcon>
                                <AddCircleIcon color='primary' />
                            </ListItemIcon>
                            <ListItemText primary='Add Project' />
                        </ListItemButton>
                    </List>
                </Grid>
            </Drawer>
            <CreateProjectDialog />
        </div>
    );
}