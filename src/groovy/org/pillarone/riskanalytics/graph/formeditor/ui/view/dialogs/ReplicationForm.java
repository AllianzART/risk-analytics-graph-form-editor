package org.pillarone.riskanalytics.graph.formeditor.ui.view.dialogs;

import org.pillarone.riskanalytics.graph.core.graph.model.AbstractGraphModel;
import org.pillarone.riskanalytics.graph.formeditor.ui.model.AvailablePortsComboBoxModel;
import org.pillarone.riskanalytics.graph.formeditor.ui.model.ReplicationFormModel;

public class ReplicationForm extends AbstractRegistryFormBuilder<ReplicationFormModel> {

    AbstractGraphModel fGraphModel;

    public ReplicationForm(ReplicationFormModel formModel, AbstractGraphModel model) {
        super(formModel);
        fGraphModel = model;
    }

    @Override
    protected void initForm() {
        setColumnWeights(0f, 0f, 1f);
        addComboBox("inner", new AvailablePortsComboBoxModel(fGraphModel));
        addTextField("outer").columns(15);
    }
}