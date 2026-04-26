package app.gearaddict.views.curation;

import app.gearaddict.equipment.CatalogException;
import app.gearaddict.equipment.Manufacturer;
import app.gearaddict.equipment.ManufacturerService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;

class ManufacturerFormDialog extends Dialog {

    private final transient ManufacturerService manufacturerService;
    private final Long curatorId;
    private final Runnable onSaved;

    private final TextField nameField = new TextField("Name");

    private Long manufacturerId;

    ManufacturerFormDialog(ManufacturerService manufacturerService,
                           Long curatorId,
                           Runnable onSaved) {
        this.manufacturerService = manufacturerService;
        this.curatorId = curatorId;
        this.onSaved = onSaved;

        setId("manufacturer-form-dialog");
        setWidth("420px");

        nameField.setId("manufacturer-form-name");
        nameField.setRequiredIndicatorVisible(true);
        nameField.setMinLength(ManufacturerService.MIN_NAME_LENGTH);
        nameField.setMaxLength(ManufacturerService.MAX_NAME_LENGTH);
        nameField.setHelperText(ManufacturerService.MIN_NAME_LENGTH
                + "–" + ManufacturerService.MAX_NAME_LENGTH + " characters.");
        nameField.setWidthFull();

        VerticalLayout content = new VerticalLayout(nameField);
        content.setPadding(false);
        content.setSpacing(true);
        add(content);

        Button save = new Button("Save", e -> save());
        save.setId("manufacturer-form-save");
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button cancel = new Button("Cancel", e -> close());
        cancel.setId("manufacturer-form-cancel");

        getFooter().add(cancel, save);
    }

    void openForAdd() {
        manufacturerId = null;
        setHeaderTitle("Add Manufacturer");
        nameField.clear();
        clearErrors();
        open();
    }

    void openForEdit(Manufacturer manufacturer) {
        manufacturerId = manufacturer.id();
        setHeaderTitle("Edit Manufacturer");
        nameField.setValue(manufacturer.name());
        clearErrors();
        open();
    }

    private void save() {
        clearErrors();
        try {
            if (manufacturerId == null) {
                manufacturerService.create(curatorId, nameField.getValue());
                Notification.show("Manufacturer added.", 3000, Notification.Position.TOP_CENTER)
                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            } else {
                manufacturerService.rename(curatorId, manufacturerId, nameField.getValue());
                Notification.show("Manufacturer updated.", 3000, Notification.Position.TOP_CENTER)
                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            }
            close();
            onSaved.run();
        } catch (CatalogException ex) {
            handle(ex);
        }
    }

    private void handle(CatalogException ex) {
        switch (ex.reason()) {
            case NAME_REQUIRED, NAME_TOO_SHORT, NAME_TOO_LONG, DUPLICATE_MANUFACTURER -> {
                nameField.setInvalid(true);
                nameField.setErrorMessage(ex.getMessage());
            }
            case NOT_FOUND -> {
                Notification.show(ex.getMessage(), 4000, Notification.Position.TOP_CENTER)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
                close();
                onSaved.run();
            }
            default -> Notification.show(ex.getMessage(), 4000, Notification.Position.TOP_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    private void clearErrors() {
        nameField.setInvalid(false);
    }
}
