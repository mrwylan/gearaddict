package app.gearaddict.views.curation;

import app.gearaddict.equipment.CatalogException;
import app.gearaddict.equipment.Equipment;
import app.gearaddict.equipment.EquipmentCategory;
import app.gearaddict.equipment.EquipmentFormData;
import app.gearaddict.equipment.EquipmentService;
import app.gearaddict.equipment.Manufacturer;
import app.gearaddict.equipment.ManufacturerService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;

import java.util.List;
import java.util.function.LongConsumer;

class EquipmentFormDialog extends Dialog {

    private final transient EquipmentService equipmentService;
    private final transient ManufacturerService manufacturerService;
    private final Long curatorId;
    private final Runnable onSaved;
    private final LongConsumer onOpenExisting;

    private final TextField nameField = new TextField("Name");
    private final ComboBox<Manufacturer> manufacturerCombo = new ComboBox<>("Manufacturer");
    private final Select<EquipmentCategory> categorySelect = new Select<>();
    private final TextArea descriptionField = new TextArea("Description");

    private Long equipmentId;

    EquipmentFormDialog(EquipmentService equipmentService,
                        ManufacturerService manufacturerService,
                        Long curatorId,
                        Runnable onSaved,
                        LongConsumer onOpenExisting) {
        this.equipmentService = equipmentService;
        this.manufacturerService = manufacturerService;
        this.curatorId = curatorId;
        this.onSaved = onSaved;
        this.onOpenExisting = onOpenExisting;

        setId("equipment-form-dialog");
        setWidth("520px");

        configureFields();

        Button save = new Button("Save", e -> save());
        save.setId("equipment-form-save");
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button cancel = new Button("Cancel", e -> close());
        cancel.setId("equipment-form-cancel");

        getFooter().add(cancel, save);

        VerticalLayout content = new VerticalLayout(
                nameField, manufacturerCombo, categorySelect, descriptionField);
        content.setPadding(false);
        content.setSpacing(true);
        add(content);
    }

    private void configureFields() {
        nameField.setId("equipment-form-name");
        nameField.setRequiredIndicatorVisible(true);
        nameField.setMaxLength(EquipmentService.MAX_NAME_LENGTH);
        nameField.setWidthFull();

        manufacturerCombo.setId("equipment-form-manufacturer");
        manufacturerCombo.setRequiredIndicatorVisible(true);
        manufacturerCombo.setItemLabelGenerator(Manufacturer::name);
        manufacturerCombo.setWidthFull();

        categorySelect.setId("equipment-form-category");
        categorySelect.setLabel("Category");
        categorySelect.setRequiredIndicatorVisible(true);
        categorySelect.setItems(List.of(EquipmentCategory.values()));
        categorySelect.setItemLabelGenerator(EquipmentCategory::label);
        categorySelect.setWidthFull();

        descriptionField.setId("equipment-form-description");
        descriptionField.setMaxLength(EquipmentService.MAX_DESCRIPTION_LENGTH);
        descriptionField.setHelperText("Optional. Up to "
                + EquipmentService.MAX_DESCRIPTION_LENGTH + " characters.");
        descriptionField.setWidthFull();
    }

    void openForAdd() {
        equipmentId = null;
        setHeaderTitle("Add Equipment");
        manufacturerCombo.setItems(manufacturerService.findAll());
        nameField.clear();
        manufacturerCombo.clear();
        categorySelect.clear();
        descriptionField.clear();
        clearErrors();
        open();
    }

    void openForEdit(Equipment equipment) {
        equipmentId = equipment.id();
        setHeaderTitle("Edit Equipment");
        manufacturerCombo.setItems(manufacturerService.findAll());
        nameField.setValue(equipment.name());
        manufacturerService.findById(equipment.manufacturerId())
                .ifPresent(manufacturerCombo::setValue);
        categorySelect.setValue(equipment.category());
        descriptionField.setValue(equipment.description() == null ? "" : equipment.description());
        clearErrors();
        open();
    }

    private void save() {
        clearErrors();
        Manufacturer manufacturer = manufacturerCombo.getValue();
        EquipmentFormData form = new EquipmentFormData(
                nameField.getValue(),
                manufacturer == null ? null : manufacturer.id(),
                categorySelect.getValue(),
                descriptionField.getValue());

        try {
            if (equipmentId == null) {
                equipmentService.create(curatorId, form);
                Notification.show("Equipment added.", 3000, Notification.Position.TOP_CENTER)
                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            } else {
                equipmentService.update(curatorId, equipmentId, form);
                Notification.show("Equipment updated.", 3000, Notification.Position.TOP_CENTER)
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
            case NAME_REQUIRED, NAME_TOO_LONG -> {
                nameField.setInvalid(true);
                nameField.setErrorMessage(ex.getMessage());
            }
            case MANUFACTURER_REQUIRED -> {
                manufacturerCombo.setInvalid(true);
                manufacturerCombo.setErrorMessage(ex.getMessage());
            }
            case CATEGORY_REQUIRED -> {
                categorySelect.setInvalid(true);
                categorySelect.setErrorMessage(ex.getMessage());
            }
            case DESCRIPTION_TOO_LONG -> {
                descriptionField.setInvalid(true);
                descriptionField.setErrorMessage(ex.getMessage());
            }
            case DUPLICATE_EQUIPMENT -> {
                nameField.setInvalid(true);
                nameField.setErrorMessage(ex.getMessage());
                if (ex.conflictingId() != null && onOpenExisting != null) {
                    Notification notification = Notification.show(
                            ex.getMessage() + " Opening the existing entry…",
                            4000, Notification.Position.TOP_CENTER);
                    notification.addThemeVariants(NotificationVariant.LUMO_WARNING);
                    Long conflict = ex.conflictingId();
                    close();
                    onOpenExisting.accept(conflict);
                }
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
        manufacturerCombo.setInvalid(false);
        categorySelect.setInvalid(false);
        descriptionField.setInvalid(false);
    }
}
