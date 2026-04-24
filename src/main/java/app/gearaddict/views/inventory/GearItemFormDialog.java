package app.gearaddict.views.inventory;

import app.gearaddict.equipment.Equipment;
import app.gearaddict.equipment.EquipmentCategory;
import app.gearaddict.equipment.EquipmentService;
import app.gearaddict.gear.GearItem;
import app.gearaddict.gear.GearItemException;
import app.gearaddict.gear.GearItemFormData;
import app.gearaddict.gear.GearItemService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;

import java.util.List;
import java.util.Optional;

class GearItemFormDialog extends Dialog {

    private final transient GearItemService gearItemService;
    private final transient EquipmentService equipmentService;
    private final Long userId;
    private final Runnable onSaved;

    private final ComboBox<Equipment> equipmentCombo = new ComboBox<>("Search equipment catalog");
    private final TextField deviceNameField = new TextField("Device name");
    private final Select<EquipmentCategory> categorySelect = new Select<>();
    private final TextArea notesField = new TextArea("Personal notes");

    private Long gearItemId;

    GearItemFormDialog(GearItemService gearItemService,
                       EquipmentService equipmentService,
                       Long userId,
                       Runnable onSaved) {
        this.gearItemService = gearItemService;
        this.equipmentService = equipmentService;
        this.userId = userId;
        this.onSaved = onSaved;

        setId("gear-form-dialog");
        setHeaderTitle("Add Gear Item");
        setWidth("480px");

        configureFields();

        Button save = new Button("Save", e -> save());
        save.setId("gear-form-save");
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button cancel = new Button("Cancel", e -> close());
        cancel.setId("gear-form-cancel");

        getFooter().add(cancel, save);

        VerticalLayout content = new VerticalLayout(
                equipmentCombo,
                deviceNameField,
                categorySelect,
                notesField);
        content.setPadding(false);
        content.setSpacing(true);
        add(content);
    }

    private void configureFields() {
        equipmentCombo.setId("gear-form-equipment");
        equipmentCombo.setPlaceholder("e.g. Minimoog, Korg...");
        equipmentCombo.setClearButtonVisible(true);
        equipmentCombo.setItemLabelGenerator(e -> e.manufacturer() + " — " + e.name());
        equipmentCombo.setItems(
                query -> {
                    String filter = query.getFilter().orElse("").trim();
                    Optional<String> search = filter.length() >= EquipmentService.MIN_SEARCH_TERM_LENGTH
                            ? Optional.of(filter) : Optional.empty();
                    return equipmentService.browse(Optional.empty(), search,
                            query.getOffset(), query.getLimit()).stream();
                });
        equipmentCombo.addValueChangeListener(event -> onEquipmentSelected(event.getValue()));
        equipmentCombo.setWidthFull();

        deviceNameField.setId("gear-form-name");
        deviceNameField.setPlaceholder("Manufacturer + model");
        deviceNameField.setHelperText("Use when the device is not in the catalog.");
        deviceNameField.setMaxLength(GearItemService.MAX_NAME_LENGTH);
        deviceNameField.setClearButtonVisible(true);
        deviceNameField.setWidthFull();

        categorySelect.setId("gear-form-category");
        categorySelect.setLabel("Category");
        categorySelect.setRequiredIndicatorVisible(true);
        categorySelect.setItems(List.of(EquipmentCategory.values()));
        categorySelect.setItemLabelGenerator(EquipmentCategory::label);
        categorySelect.setWidthFull();

        notesField.setId("gear-form-notes");
        notesField.setPlaceholder("Your experience, condition, mods...");
        notesField.setMaxLength(GearItemService.MAX_NOTES_LENGTH);
        notesField.setHelperText("Up to " + GearItemService.MAX_NOTES_LENGTH + " characters.");
        notesField.setWidthFull();
    }

    private void onEquipmentSelected(Equipment equipment) {
        if (equipment == null) {
            deviceNameField.setReadOnly(false);
            return;
        }
        deviceNameField.clear();
        deviceNameField.setReadOnly(true);
        if (categorySelect.isEmpty()) {
            categorySelect.setValue(equipment.category());
        }
    }

    void openForAdd() {
        gearItemId = null;
        setHeaderTitle("Add Gear Item");
        equipmentCombo.clear();
        deviceNameField.clear();
        deviceNameField.setReadOnly(false);
        categorySelect.clear();
        notesField.clear();
        clearErrors();
        open();
    }

    void openForEdit(GearItem gearItem) {
        gearItemId = gearItem.id();
        setHeaderTitle("Edit Gear Item");
        clearErrors();

        if (gearItem.equipmentId() != null) {
            Equipment equipment = equipmentService.findById(gearItem.equipmentId()).orElse(null);
            equipmentCombo.setValue(equipment);
            deviceNameField.clear();
            deviceNameField.setReadOnly(equipment != null);
        } else {
            equipmentCombo.clear();
            deviceNameField.setReadOnly(false);
            deviceNameField.setValue(gearItem.name() == null ? "" : gearItem.name());
        }
        categorySelect.setValue(gearItem.category());
        notesField.setValue(gearItem.notes() == null ? "" : gearItem.notes());
        open();
    }

    private void save() {
        clearErrors();
        GearItemFormData form = currentForm();

        if (form.category() == null) {
            categorySelect.setInvalid(true);
            categorySelect.setErrorMessage("Category is required.");
            return;
        }
        if (form.equipmentId() == null && (form.freeTextName() == null || form.freeTextName().isBlank())) {
            deviceNameField.setInvalid(true);
            deviceNameField.setErrorMessage("Select a catalog entry or enter a device name.");
            return;
        }

        if (gearItemId == null && gearItemService.isDuplicateForUser(userId, form.equipmentId(), form.freeTextName())) {
            confirmDuplicateAndPersist(form);
            return;
        }

        persist(form);
    }

    private void confirmDuplicateAndPersist(GearItemFormData form) {
        ConfirmDialog confirm = new ConfirmDialog();
        confirm.setId("gear-form-duplicate-confirm");
        confirm.setHeader("Already in your inventory");
        confirm.setText("You already own this item. Add another copy?");
        confirm.setCancelable(true);
        confirm.setConfirmText("Add another");
        confirm.setCancelText("Cancel");
        confirm.addConfirmListener(e -> persist(form));
        confirm.open();
    }

    private void persist(GearItemFormData form) {
        try {
            if (gearItemId == null) {
                gearItemService.add(userId, form);
                Notification.show("Gear item added.", 3000, Notification.Position.TOP_CENTER)
                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            } else {
                gearItemService.update(userId, gearItemId, form);
                Notification.show("Gear item updated.", 3000, Notification.Position.TOP_CENTER)
                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            }
            close();
            onSaved.run();
        } catch (GearItemException ex) {
            handleError(ex);
        }
    }

    private GearItemFormData currentForm() {
        Equipment selected = equipmentCombo.getValue();
        String freeText = deviceNameField.getValue();
        return new GearItemFormData(
                selected == null ? null : selected.id(),
                selected == null ? freeText : null,
                categorySelect.getValue(),
                notesField.getValue());
    }

    private void handleError(GearItemException ex) {
        switch (ex.reason()) {
            case CATEGORY_REQUIRED -> {
                categorySelect.setInvalid(true);
                categorySelect.setErrorMessage("Category is required.");
            }
            case EQUIPMENT_OR_NAME_REQUIRED -> {
                deviceNameField.setInvalid(true);
                deviceNameField.setErrorMessage("Select a catalog entry or enter a device name.");
            }
            case NAME_TOO_LONG -> {
                deviceNameField.setInvalid(true);
                deviceNameField.setErrorMessage(
                        "Must be at most " + GearItemService.MAX_NAME_LENGTH + " characters.");
            }
            case NOTES_TOO_LONG -> {
                notesField.setInvalid(true);
                notesField.setErrorMessage(
                        "Must be at most " + GearItemService.MAX_NOTES_LENGTH + " characters.");
            }
            case NOT_OWNER, NOT_FOUND -> {
                Notification.show(ex.getMessage(), 4000, Notification.Position.TOP_CENTER)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
                close();
            }
        }
    }

    private void clearErrors() {
        equipmentCombo.setInvalid(false);
        deviceNameField.setInvalid(false);
        categorySelect.setInvalid(false);
        notesField.setInvalid(false);
    }
}
