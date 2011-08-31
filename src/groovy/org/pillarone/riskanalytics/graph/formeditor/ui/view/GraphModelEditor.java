package org.pillarone.riskanalytics.graph.formeditor.ui.view;


import com.canoo.ulc.detachabletabbedpane.server.ITabListener;
import com.canoo.ulc.detachabletabbedpane.server.TabEvent;
import com.canoo.ulc.detachabletabbedpane.server.ULCCloseableTabbedPane;
import com.canoo.ulc.graph.ULCGraphPalette;
import com.ulcjava.applicationframework.application.*;
import com.ulcjava.applicationframework.application.ApplicationContext;
import com.ulcjava.base.application.*;
import com.ulcjava.base.application.event.ActionEvent;
import com.ulcjava.base.application.event.IActionListener;
import com.ulcjava.base.application.event.KeyEvent;
import com.ulcjava.base.application.util.*;
import com.ulcjava.base.shared.FileChooserConfig;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.groovy.grails.commons.ApplicationHolder;
import org.pillarone.riskanalytics.core.components.ComposedComponent;
import org.pillarone.riskanalytics.core.model.Model;
import org.pillarone.riskanalytics.core.simulation.item.Parameterization;
import org.pillarone.riskanalytics.graph.core.graph.model.AbstractGraphModel;
import org.pillarone.riskanalytics.graph.core.graph.model.ComposedComponentGraphModel;
import org.pillarone.riskanalytics.graph.core.graph.model.ModelGraphModel;
import org.pillarone.riskanalytics.graph.core.graph.persistence.GraphPersistenceService;
import org.pillarone.riskanalytics.graph.core.graphimport.AbstractGraphImport;
import org.pillarone.riskanalytics.graph.core.graphimport.ComposedComponentGraphImport;
import org.pillarone.riskanalytics.graph.core.graphimport.GraphImportService;
import org.pillarone.riskanalytics.graph.core.graphimport.ModelGraphImport;
import org.pillarone.riskanalytics.graph.formeditor.ui.model.TypeDefinitionFormModel;
import org.pillarone.riskanalytics.graph.formeditor.ui.model.beans.TypeDefinitionBean;
import org.pillarone.riskanalytics.graph.formeditor.util.GraphModelUtilities;
import org.pillarone.riskanalytics.graph.formeditor.util.ParameterUtilities;

import java.io.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * The main window with the form editor view.
 * It inherits from {@link AbstractBean} to benefit from its property change support.
 * <p/>
 * The window is a tabbed pane which contains for each model or component to be edited a tab.
 *
 * @author martin.melchior
 */
public class GraphModelEditor extends AbstractBean {

    private static Log LOG = LogFactory.getLog(GraphModelEditor.class);

    /* Context is needed to load resources (such as icons, etc).*/
    private ApplicationContext fContext;
    /* The editor view.*/
    private ULCCloseableTabbedPane fEditorArea;
    /* Set of currently opened type defs - check that type defs does not already exist. */
    private Set<TypeDefinitionBean> fEditedTypeDefinitions;
    /* Is used for remembering what types have already been declared */
    private Map<ULCComponent, SingleModelMultiEditView> fModelTabs;
    /* A dialog for models or composed components to be imported.*/
    private TypeImportDialog fTypeImportView;

    private ModelRepositoryTree fModelRepositoryTree;

    private GraphPersistenceService fPersistenceService;

    /**
     * @param context Application context is used for accessing and using resources (such as icons, etc.).
     */
    public GraphModelEditor(ApplicationContext context) {
        fContext = context;
        fEditedTypeDefinitions = new HashSet<TypeDefinitionBean>();
        fModelTabs = new HashMap<ULCComponent, SingleModelMultiEditView>();
    }

    /**
     * Returns the split pane - consisting of the palette view and the model edit view in tabbed pane -
     * as content view.
     *
     * @return model edit and palette view
     */
    public ULCComponent getContentView() {
        ULCBoxPane modelEdit = new ULCBoxPane(true);
        modelEdit.setPreferredSize(new Dimension(600, 600));
        modelEdit.add(ULCBoxPane.BOX_EXPAND_BOTTOM, ULCFiller.createVerticalStrut(3));
        ULCSeparator separator = new ULCSeparator();
        separator.setBorder(BorderFactory.createEmptyBorder(0, 15, 0, 15));
        modelEdit.add(ULCBoxPane.BOX_EXPAND_BOTTOM, separator);

        fEditorArea = new ULCCloseableTabbedPane();
        fEditorArea.addTabListener(new ITabListener() {
            public void tabClosing(TabEvent event) {
                event.getClosableTabbedPane().closeCloseableTab(event.getTabClosingIndex());
                if (fEditorArea.getTabCount() > 0) {
                    event.getClosableTabbedPane().setSelectedIndex(0);
                }
            }
        });
        modelEdit.add(ULCBoxPane.BOX_EXPAND_EXPAND, fEditorArea);

        ULCBoxPane palettePane = new ULCBoxPane(1, 2);
        TabularFilterView filterView = new TabularFilterView();
        ULCBoxPane paletteArea = getPalettePane(filterView);
        palettePane.add(ULCBoxPane.BOX_LEFT_TOP, filterView.getContent());
        palettePane.add(ULCBoxPane.BOX_EXPAND_EXPAND, new ULCScrollPane(paletteArea));

        ULCBoxPane repositoryTreePane = new ULCBoxPane(true);
        repositoryTreePane.setPreferredSize(new Dimension(200, 200));
        fModelRepositoryTree = new ModelRepositoryTree(this);
        repositoryTreePane.add(ULCBoxPane.BOX_EXPAND_EXPAND, fModelRepositoryTree);

        ULCSplitPane typeSelectionPane = new ULCSplitPane(ULCSplitPane.VERTICAL_SPLIT);
        typeSelectionPane.setPreferredSize(new Dimension(150, 600));
        typeSelectionPane.setTopComponent(palettePane);
        typeSelectionPane.setBottomComponent(new ULCScrollPane(repositoryTreePane));
        typeSelectionPane.setOneTouchExpandable(true);
        typeSelectionPane.setDividerLocation(0.8);
        typeSelectionPane.setDividerSize(10);

        ULCSplitPane splitPane = new ULCSplitPane(ULCSplitPane.HORIZONTAL_SPLIT);
        splitPane.setPreferredSize(new Dimension(800, 600));
        splitPane.setLeftComponent(typeSelectionPane);
        splitPane.setRightComponent(new ULCScrollPane(modelEdit));
        splitPane.setOneTouchExpandable(true);
        splitPane.setDividerLocation(0.3);
        splitPane.setDividerSize(10);

        return splitPane;
    }

    private ULCBoxPane getPalettePane(TabularFilterView filterView) {
        ULCBoxPane viewSelector = new ULCBoxPane(false);
        ULCRadioButton categoryTreeSelectButton = new ULCRadioButton("Categories", true);
        ULCRadioButton packageSelectButton = new ULCRadioButton("Package");
        ULCRadioButton alphabeticalButton = new ULCRadioButton("Alphabetical");
        ULCButtonGroup buttonGroup = new ULCButtonGroup();
        packageSelectButton.setGroup(buttonGroup);
        categoryTreeSelectButton.setGroup(buttonGroup);
        alphabeticalButton.setGroup(buttonGroup);

        viewSelector.add(ULCBoxPane.BOX_LEFT_CENTER, categoryTreeSelectButton);
        viewSelector.add(ULCBoxPane.BOX_LEFT_CENTER, alphabeticalButton);
        viewSelector.add(ULCBoxPane.BOX_LEFT_CENTER, packageSelectButton);
        viewSelector.add(ULCBoxPane.BOX_EXPAND_EXPAND, new ULCFiller());

        final ULCCardPane views = new ULCCardPane();

        final ComponentCategoryTree categoryTree = new ComponentCategoryTree(this);
        filterView.addSearchListener(categoryTree);
        views.addCard("categoryTree", categoryTree);

        final ComponentTypeTree packagePalette = new ComponentTypeTree(this);
        filterView.addSearchListener(packagePalette);
        views.addCard("packagePalette", packagePalette);

        final SortedComponentDefinitionsTree alphabeticalPalette = new SortedComponentDefinitionsTree(this);
        filterView.addSearchListener(alphabeticalPalette);
        views.addCard("alphabeticalPalette", alphabeticalPalette);

        categoryTreeSelectButton.addActionListener(new IActionListener() {
            public void actionPerformed(ActionEvent event) {
                views.setSelectedComponent(categoryTree);
            }
        });

        packageSelectButton.addActionListener(new IActionListener() {
            public void actionPerformed(ActionEvent event) {
                views.setSelectedComponent(packagePalette);
            }
        });

        alphabeticalButton.addActionListener(new IActionListener() {
            public void actionPerformed(ActionEvent event) {
                views.setSelectedComponent(alphabeticalPalette);
            }
        });
        ULCBoxPane paletteArea = new ULCBoxPane(true);
        paletteArea.add(ULCBoxPane.BOX_EXPAND_TOP, viewSelector);
        paletteArea.add(ULCBoxPane.BOX_EXPAND_EXPAND, views);

        return paletteArea;
    }


    /**
     * Creates a new tab for a given or a new model with given type definition
     *
     * @param model
     * @param typeDef
     */
    private void addModelToView(AbstractGraphModel model, TypeDefinitionBean typeDef, boolean isEditable) {
        SingleModelMultiEditView modelView = new SingleModelMultiEditView(fContext, model);
        fModelTabs.put(modelView.getView(), modelView);
        fEditorArea.addTab(typeDef.getName(), modelView.getView());
        fEditorArea.setSelectedIndex(fEditorArea.getComponentCount() - 1);
        fEditorArea.setToolTipTextAt(fEditorArea.getComponentCount() - 1, model.getPackageName() + "." + model.getName());
        fEditedTypeDefinitions.add(typeDef);
        //fModelRepositoryTree.getTreeModel().addNode(model);
    }

    private void addParameterSet(Parameterization p, String name) {
        ULCComponent comp = fEditorArea.getSelectedComponent();
        if (comp != null && fModelTabs.containsKey(comp)) {
            SingleModelMultiEditView modelView = fModelTabs.get(comp);
            modelView.addParameterSet(p, name);
        } else {
            ULCAlert alert = new ULCAlert("No model view available", "Create or load the model view before you ingest the parametrization.", "ok");
            alert.show();
        }
    }

    /**
     * Show the type definition dialog - create the dialog if not yet instantiated
     */
    private void showTypeDefinitionDialog() {
        final TypeDefinitionDialog fTypeDefView = new TypeDefinitionDialog(UlcUtilities.getWindowAncestor(fEditorArea), fEditedTypeDefinitions);
        IActionListener newModelListener = new IActionListener() {

            public void actionPerformed(ActionEvent event) {
                TypeDefinitionFormModel typeDefinitionFormModel = fTypeDefView.getBeanForm().getModel();
                if (typeDefinitionFormModel.hasErrors()) return;
                TypeDefinitionBean typeDef = typeDefinitionFormModel.getBean();
                AbstractGraphModel model = typeDef.getBaseType().equals("Model") ? new ModelGraphModel() : new ComposedComponentGraphModel();
                model.setPackageName(typeDef.getPackageName());
                model.setName(typeDef.getName());
                addModelToView(model, typeDef, true);
                fTypeDefView.setVisible(false);
            }
        };
        fTypeDefView.getBeanForm().addSaveActionListener(newModelListener);
        KeyStroke enter = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0, false);
        fTypeDefView.getTypeDefinitionForm().registerKeyboardAction(enter, newModelListener);
        fTypeDefView.getTypeDefinitionForm().addKeyListener();
        fTypeDefView.setVisible(true);
        ULCComponent nameTextField = fTypeDefView.getTypeDefinitionForm().getComponent("name");
        if (nameTextField != null)
            nameTextField.requestFocus();
    }

    /**
     * Import the component type specified by <code>clazzName</code>.
     * It means that for a given class (of type <code>ComposedComponent</code> or <code>AbstractModel</code>
     * a corresponding graph model is created and added to the model edit pane (as a new tab).
     *
     * @param clazzName name of the class to be imported
     * @return boolean to indicate whether the import was successful.
     * @throws ClassNotFoundException
     */
    public boolean importComponentType(String clazzName) throws ClassNotFoundException {
        Class clazz = getClass().getClassLoader().loadClass(clazzName);
        AbstractGraphImport importer = null;
        if (ComposedComponent.class.isAssignableFrom(clazz)) {
            importer = new ComposedComponentGraphImport();
        } else if (Model.class.isAssignableFrom(clazz)) {
            importer = new ModelGraphImport();
        }
        if (importer != null) {
            AbstractGraphModel model = importer.importGraph(clazz, null);
            TypeDefinitionBean typeDef = new TypeDefinitionBean();
            typeDef.setBaseType(model instanceof ModelGraphModel ? "Model" : "ComposedComponent");
            typeDef.setName(model.getName());
            typeDef.setPackageName(model.getPackageName());
            addModelToView(model, typeDef, false);
            return true;
        }
        return false;
    }

    /**
     * Action for creating a new graph model and preparing a new tab in the editor pane.
     * Delegates to showTypDefinitionDialog.
     */
    @Action
    public void newModelAction() {
        showTypeDefinitionDialog();
    }

    /**
     * Deploys the model in RA application.
     */
    @Action
    public void exportToApplication() {
        if (fEditorArea.getComponentCount() == 0) {
            ULCAlert alert = new ULCAlert("No Model found", "No model or component to be saved", "ok");
            alert.show();
        } else {
            int i = fEditorArea.getSelectedIndex();
            ULCComponent component = fEditorArea.getComponentAt(i);
            if (fModelTabs.containsKey(component)) {
                AbstractGraphModel model = fModelTabs.get(component).getGraphModel();
                GraphModelUtilities.exportToApplication((ModelGraphModel) model);
            } else {
                ULCAlert alert = new ULCAlert("No Model found", "Current page does not contain a model or component specification.", "ok");
                alert.show();
            }

        }
    }

    @SuppressWarnings("serial")
    @Action
    public void importModelAction() {
        FileChooserConfig config = new FileChooserConfig();
        config.setDialogTitle("Open file");
        config.setDialogType(FileChooserConfig.FILES_ONLY);
        config.addFileFilterConfig(new FileChooserConfig.FileFilterConfig(
                new String[]{"groovy"}, "groovy files (*.groovy)")
        );

        IFileLoadHandler handler = new IFileLoadHandler() {
            public void onSuccess(InputStream[] inputStreams, String[] filePaths, String[] fileNames) {
                try {
                    InputStream in = inputStreams[0];
                    Writer writer = new StringWriter();
                    char[] buffer = new char[1024];
                    try {
                        Reader reader = new BufferedReader(new InputStreamReader(in, "UTF-8"));
                        int n;
                        while ((n = reader.read(buffer)) != -1) {
                            writer.write(buffer, 0, n);
                        }
                    } finally {
                        in.close();
                    }
                    String content = writer.toString();
                    GraphImportService importService = new GraphImportService();
                    AbstractGraphModel model = importService.importGraph(content);
                    TypeDefinitionBean typeDef = new TypeDefinitionBean();
                    typeDef.setBaseType(model instanceof ModelGraphModel ? "Model" : "Composed Component");
                    typeDef.setName(model.getName());
                    typeDef.setPackageName(model.getPackageName());
                    addModelToView(model, typeDef, true);
                } catch (Exception ex) {
                    new ULCAlert(UlcUtilities.getWindowAncestor(fEditorArea), "Import failed", "The specified file could not be imported. Reason: " + ex.getMessage(), "Ok").show();
                }
            }

            public void onFailure(int reason, String description) {
                new ULCAlert(UlcUtilities.getWindowAncestor(fEditorArea), "Import failed", "The specified file could not be imported.", "Ok").show();
            }
        };

        ClientContext.loadFile(handler, config, fEditorArea);
    }

    public void loadModel(String name, String packageName) {
        TypeDefinitionBean typeDefBean = new TypeDefinitionBean();
        final AbstractGraphModel graphModel = getPersistenceService().load(name, packageName);
        typeDefBean.setName(graphModel.getName());
        typeDefBean.setPackageName(graphModel.getPackageName());
        typeDefBean.setBaseType(graphModel instanceof ModelGraphModel ? "Model" : "ComposedComponent");
        addModelToView(graphModel, typeDefBean, true);
    }

    @Action
    public void createParametersAction() {
        ULCComponent comp = fEditorArea.getSelectedComponent();
        if (comp != null) {
            fModelTabs.get(comp).addParameterSet(null);
        }
    }

    @Action
    public void simulateAction() {
        ULCComponent comp = fEditorArea.getSelectedComponent();
        if (comp != null) {
            fModelTabs.get(comp).simulateAction(false);
        }
    }

    @Action
    public void saveModelAction() {
        AbstractGraphModel model = getSelectedModel();
        if (!fModelRepositoryTree.getTreeModel().containsModel(model)) {
            fModelRepositoryTree.getTreeModel().addNode(model);
        }
        getPersistenceService().save(model);
    }

    private GraphPersistenceService getPersistenceService() {
        if (fPersistenceService == null) {
            org.springframework.context.ApplicationContext ctx = ApplicationHolder.getApplication().getMainContext();
            fPersistenceService = ctx.getBean(GraphPersistenceService.class);
        }
        return fPersistenceService;
    }

    @Action
    public void exportModelToGroovyAction() {
        AbstractGraphModel model = getSelectedModel();
        String text = GraphModelUtilities.getGroovyModelCode(model);
        saveOutput(model.getName() + ".groovy", text, UlcUtilities.getWindowAncestor(fModelRepositoryTree));
    }

    private AbstractGraphModel getSelectedModel() {
        ULCComponent comp = fEditorArea.getSelectedComponent();
        AbstractGraphModel model = fModelTabs.get(comp).getGraphModel();
        return model;
    }

    private void saveOutput(String name, final String text, final ULCWindow ancestor) {
        FileChooserConfig config = new FileChooserConfig();
        config.setDialogTitle("Save file as");
        config.setDialogType(FileChooserConfig.SAVE_DIALOG);
        config.setSelectedFile(name);

        IFileChooseHandler chooser = new IFileChooseHandler() {
            public void onSuccess(String[] filePaths, String[] fileNames) {
                String selectedFile = filePaths[0];
                IFileStoreHandler fileStoreHandler =
                        new IFileStoreHandler() {
                            public void prepareFile(java.io.OutputStream stream) throws Exception {
                                try {
                                    stream.write(text.getBytes());
                                } catch (UnsupportedOperationException t) {
                                    new ULCAlert(ancestor, "Export failed", t.getMessage(), "Ok").show();
                                } catch (Throwable t) {
                                    new ULCAlert(ancestor, "Export failed", t.getMessage(), "Ok").show();
                                } finally {
                                    stream.close();
                                }
                            }

                            public void onSuccess(String filePath, String fileName) {
                            }

                            public void onFailure(int reason, String description) {
//	        			new ULCAlert(ancestor, "Export failed", description, "Ok").show();
                            }
                        };
                try {
                    ClientContext.storeFile(fileStoreHandler, selectedFile);
                } catch (Exception ex) {

                }
            }

            public void onFailure(int reason, String description) {
                new ULCAlert(ancestor, "Export failed", description, "ok").show();
            }
        };
        ClientContext.chooseFile(chooser, config, ancestor);
    }

    @Action
    public void exportModelToApplicationAction() {
        AbstractGraphModel model = getSelectedModel();
        try {
            GraphModelUtilities.exportToApplication(model);
        } catch (Exception ex) {
            ULCAlert alert = new ULCAlert("Model not deployed.", "Model could not be deployed. Reason: " + ex.getMessage(), "ok");
            alert.show();
            LOG.error("Model could not be deployed", ex);
        }

    }

    protected ApplicationActionMap getActionMap() {
        return fContext.getActionMap(this);
    }

    public ULCToolBar getToolBar() {
        return new ToolBarFactory(getActionMap()).createToolBar("newModelAction", "importModelAction", "saveModelAction", "exportModelToGroovyAction", "exportModelToApplicationAction", "createParametersAction", "importParametersAction", "exportParametersAction", "simulateAction");
    }

    @Action
    public void importParametersAction() {
        FileChooserConfig config = new FileChooserConfig();
        config.setDialogTitle("Choose Parameter File");
        config.setDialogType(FileChooserConfig.FILES_ONLY);
        config.addFileFilterConfig(new FileChooserConfig.FileFilterConfig(
                new String[]{"groovy"}, "groovy files (*.groovy)")
        );

        IFileLoadHandler handler = new IFileLoadHandler() {
            public void onSuccess(InputStream[] inputStreams, String[] filePaths, String[] fileNames) {
                try {
                    InputStream in = inputStreams[0];
                    String name = fileNames[0];
                    int suffixPos = name.indexOf(".groovy");
                    name = name.substring(0, suffixPos);
                    Writer writer = new StringWriter();
                    char[] buffer = new char[1024];
                    try {
                        Reader reader = new BufferedReader(new InputStreamReader(in, "UTF-8"));
                        int n;
                        while ((n = reader.read(buffer)) != -1) {
                            writer.write(buffer, 0, n);
                        }
                    } finally {
                        in.close();
                    }
                    String content = writer.toString();
                    Parameterization params = ParameterUtilities.loadParametrization(content, name);
                    addParameterSet(params, name);
                } catch (Exception ex) {
                    new ULCAlert(UlcUtilities.getWindowAncestor(fEditorArea), "Import failed", "The specified file could not be imported. Reason: " + ex.getMessage(), "Ok").show();
                }
            }

            public void onFailure(int reason, String description) {
                new ULCAlert(UlcUtilities.getWindowAncestor(fEditorArea), "Import failed", "The specified file could not be imported.", "Ok").show();
            }
        };

        ClientContext.loadFile(handler, config, fEditorArea);
    }

    @Action
    public void exportParametersAction() {
        ULCComponent comp = fEditorArea.getSelectedComponent();
        DataTable data = fModelTabs.get(comp).getSelectedDataTable();
        if (data==null) {
            ULCAlert alert = new ULCAlert("No data selected",
                    "No data to export", "ok");
            alert.show();
            return;
        }
        String content = " "; // TODO
        saveOutput(data.getName() + ".groovy", content, UlcUtilities.getWindowAncestor(fModelRepositoryTree));
    }
}
