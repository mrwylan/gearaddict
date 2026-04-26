package app.gearaddict.views.curation;

import app.gearaddict.equipment.CatalogException;
import app.gearaddict.equipment.Equipment;
import app.gearaddict.equipment.EquipmentService;
import app.gearaddict.equipment.Manufacturer;
import app.gearaddict.equipment.ManufacturerService;
import app.gearaddict.user.User;
import app.gearaddict.user.UserService;
import app.gearaddict.views.MainLayout;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.security.AuthenticationContext;
import jakarta.annotation.security.RolesAllowed;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Optional;

@Route(value = "curation", layout = MainLayout.class)
@PageTitle("Catalog Curation — GearAddict")
@RolesAllowed("CURATOR")
public class CurationView extends VerticalLayout {

    public static final String ROUTE = "curation";

    private final transient EquipmentService equipmentService;
    private final transient ManufacturerService manufacturerService;
    private final transient UserService userService;
    private final transient AuthenticationContext authenticationContext;

    private final Tabs tabs = new Tabs();
    private final Tab equipmentTab = new Tab("Equipment");
    private final Tab manufacturersTab = new Tab("Manufacturers");

    private final VerticalLayout equipmentSection = new VerticalLayout();
    private final VerticalLayout manufacturersSection = new VerticalLayout();

    private final Grid<Equipment> equipmentGrid = new Grid<>(Equipment.class, false);
    private final Grid<Manufacturer> manufacturerGrid = new Grid<>(Manufacturer.class, false);

    private final Long curatorId;

    public CurationView(EquipmentService equipmentService,
                        ManufacturerService manufacturerService,
                        UserService userService,
                        AuthenticationContext authenticationContext) {
        this.equipmentService = equipmentService;
        this.manufacturerService = manufacturerService;
        this.userService = userService;
        this.authenticationContext = authenticationContext;
        this.curatorId = currentCurator().map(User::id).orElse(null);

        addClassName("curation-view");
        setSizeFull();
        setPadding(true);
        setSpacing(true);

        H1 heading = new H1("Catalog Curation");
        heading.setId("curation-heading");

        equipmentTab.setId("curation-tab-equipment");
        manufacturersTab.setId("curation-tab-manufacturers");
        tabs.add(equipmentTab, manufacturersTab);
        tabs.setId("curation-tabs");
        tabs.addSelectedChangeListener(e -> showSelectedSection());

        buildEquipmentSection();
        buildManufacturersSection();

        add(heading, tabs, equipmentSection, manufacturersSection);
        showSelectedSection();
    }

    private void showSelectedSection() {
        boolean equipment = tabs.getSelectedTab() == equipmentTab;
        equipmentSection.setVisible(equipment);
        manufacturersSection.setVisible(!equipment);
        if (equipment) {
            refreshEquipment();
        } else {
            refreshManufacturers();
        }
    }

    private void buildEquipmentSection() {
        equipmentSection.setId("curation-equipment-section");
        equipmentSection.setPadding(false);
        equipmentSection.setSizeFull();

        Button add = new Button("Add Equipment", VaadinIcon.PLUS.create(), e -> openEquipmentAdd());
        add.setId("add-equipment");
        add.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        HorizontalLayout actions = new HorizontalLayout(add);
        actions.setWidthFull();
        actions.setJustifyContentMode(FlexComponent.JustifyContentMode.END);

        equipmentGrid.setId("equipment-grid");
        equipmentGrid.setSizeFull();
        equipmentGrid.addColumn(Equipment::name).setHeader("Name").setAutoWidth(true).setFlexGrow(1);
        equipmentGrid.addColumn(Equipment::manufacturer).setHeader("Manufacturer").setAutoWidth(true);
        equipmentGrid.addColumn(eq -> eq.category().label()).setHeader("Category").setAutoWidth(true);
        equipmentGrid.addColumn(eq -> equipmentService.countOwners(eq.id()))
                .setHeader("Linked gear items").setAutoWidth(true);
        equipmentGrid.addComponentColumn(this::buildEquipmentActions)
                .setHeader("").setAutoWidth(true).setFlexGrow(0);

        equipmentSection.add(actions, equipmentGrid);
        equipmentSection.expand(equipmentGrid);
    }

    private void buildManufacturersSection() {
        manufacturersSection.setId("curation-manufacturers-section");
        manufacturersSection.setPadding(false);
        manufacturersSection.setSizeFull();

        Button add = new Button("Add Manufacturer", VaadinIcon.PLUS.create(), e -> openManufacturerAdd());
        add.setId("add-manufacturer");
        add.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        HorizontalLayout actions = new HorizontalLayout(add);
        actions.setWidthFull();
        actions.setJustifyContentMode(FlexComponent.JustifyContentMode.END);

        manufacturerGrid.setId("manufacturer-grid");
        manufacturerGrid.setSizeFull();
        manufacturerGrid.addColumn(Manufacturer::name).setHeader("Name").setAutoWidth(true).setFlexGrow(1);
        manufacturerGrid.addColumn(m -> manufacturerService.countEquipmentReferencing(m.id()))
                .setHeader("Equipment entries").setAutoWidth(true);
        manufacturerGrid.addComponentColumn(this::buildManufacturerActions)
                .setHeader("").setAutoWidth(true).setFlexGrow(0);

        manufacturersSection.add(actions, manufacturerGrid);
        manufacturersSection.expand(manufacturerGrid);
    }

    private Component buildEquipmentActions(Equipment equipment) {
        Button edit = new Button(VaadinIcon.EDIT.create(), e -> openEquipmentEdit(equipment));
        edit.setId("edit-equipment-" + equipment.id());
        edit.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ICON, ButtonVariant.LUMO_SMALL);
        edit.getElement().setAttribute("aria-label", "Edit " + equipment.name());

        Button remove = new Button(VaadinIcon.TRASH.create(), e -> confirmEquipmentRemoval(equipment));
        remove.setId("remove-equipment-" + equipment.id());
        remove.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ICON,
                ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_ERROR);
        remove.getElement().setAttribute("aria-label", "Remove " + equipment.name());

        HorizontalLayout layout = new HorizontalLayout(edit, remove);
        layout.setSpacing(false);
        return layout;
    }

    private Component buildManufacturerActions(Manufacturer manufacturer) {
        Button edit = new Button(VaadinIcon.EDIT.create(), e -> openManufacturerEdit(manufacturer));
        edit.setId("edit-manufacturer-" + manufacturer.id());
        edit.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ICON, ButtonVariant.LUMO_SMALL);
        edit.getElement().setAttribute("aria-label", "Edit " + manufacturer.name());

        Button remove = new Button(VaadinIcon.TRASH.create(), e -> confirmManufacturerRemoval(manufacturer));
        remove.setId("remove-manufacturer-" + manufacturer.id());
        remove.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ICON,
                ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_ERROR);
        remove.getElement().setAttribute("aria-label", "Remove " + manufacturer.name());

        HorizontalLayout layout = new HorizontalLayout(edit, remove);
        layout.setSpacing(false);
        return layout;
    }

    private void refreshEquipment() {
        equipmentGrid.setItems(equipmentService.browse(Optional.empty(), Optional.empty(), 0, Integer.MAX_VALUE));
    }

    private void refreshManufacturers() {
        manufacturerGrid.setItems(manufacturerService.findAll());
    }

    private void openEquipmentAdd() {
        if (curatorId == null) {
            return;
        }
        if (manufacturerService.findAll().isEmpty()) {
            Notification.show("Add a manufacturer first.", 4000, Notification.Position.TOP_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_WARNING);
            return;
        }
        EquipmentFormDialog dialog = new EquipmentFormDialog(
                equipmentService, manufacturerService, curatorId,
                this::refreshEquipment, this::openEquipmentEditById);
        dialog.openForAdd();
    }

    private void openEquipmentEdit(Equipment equipment) {
        if (curatorId == null) {
            return;
        }
        EquipmentFormDialog dialog = new EquipmentFormDialog(
                equipmentService, manufacturerService, curatorId,
                this::refreshEquipment, this::openEquipmentEditById);
        dialog.openForEdit(equipment);
    }

    private void openEquipmentEditById(long equipmentId) {
        equipmentService.findById(equipmentId).ifPresent(this::openEquipmentEdit);
    }

    private void confirmEquipmentRemoval(Equipment equipment) {
        if (curatorId == null) {
            return;
        }
        int linked = equipmentService.countOwners(equipment.id());

        ConfirmDialog confirm = new ConfirmDialog();
        confirm.setId("remove-equipment-confirm");
        confirm.setHeader("Remove equipment");
        String message = linked == 0
                ? "Remove \"" + equipment.name() + "\" from the catalog?"
                : "\"" + equipment.name() + "\" is linked to " + linked + " gear "
                        + (linked == 1 ? "item" : "items")
                        + ". Removing it will preserve those items as free-text entries.";
        confirm.setText(message);
        confirm.setCancelable(true);
        confirm.setConfirmText("Remove");
        confirm.setConfirmButtonTheme("error primary");
        confirm.addConfirmListener(e -> removeEquipment(equipment));
        confirm.open();
    }

    private void removeEquipment(Equipment equipment) {
        try {
            equipmentService.delete(curatorId, equipment.id());
            Notification.show("Equipment removed.", 3000, Notification.Position.TOP_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            refreshEquipment();
        } catch (CatalogException ex) {
            Notification.show(ex.getMessage(), 4000, Notification.Position.TOP_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    private void openManufacturerAdd() {
        if (curatorId == null) {
            return;
        }
        ManufacturerFormDialog dialog = new ManufacturerFormDialog(
                manufacturerService, curatorId, this::refreshManufacturers);
        dialog.openForAdd();
    }

    private void openManufacturerEdit(Manufacturer manufacturer) {
        if (curatorId == null) {
            return;
        }
        ManufacturerFormDialog dialog = new ManufacturerFormDialog(
                manufacturerService, curatorId, this::refreshManufacturers);
        dialog.openForEdit(manufacturer);
    }

    private void confirmManufacturerRemoval(Manufacturer manufacturer) {
        if (curatorId == null) {
            return;
        }
        int referencing = manufacturerService.countEquipmentReferencing(manufacturer.id());

        ConfirmDialog confirm = new ConfirmDialog();
        confirm.setId("remove-manufacturer-confirm");
        confirm.setHeader("Remove manufacturer");
        if (referencing > 0) {
            confirm.setText("\"" + manufacturer.name() + "\" cannot be removed: "
                    + referencing + " equipment "
                    + (referencing == 1 ? "entry references" : "entries reference")
                    + " it. Reassign or remove those entries first.");
            confirm.setConfirmText("Close");
            confirm.setCancelable(false);
            confirm.open();
            return;
        }
        confirm.setText("Remove \"" + manufacturer.name() + "\" from the catalog?");
        confirm.setCancelable(true);
        confirm.setConfirmText("Remove");
        confirm.setConfirmButtonTheme("error primary");
        confirm.addConfirmListener(e -> removeManufacturer(manufacturer));
        confirm.open();
    }

    private void removeManufacturer(Manufacturer manufacturer) {
        try {
            manufacturerService.delete(curatorId, manufacturer.id());
            Notification.show("Manufacturer removed.", 3000, Notification.Position.TOP_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            refreshManufacturers();
        } catch (CatalogException ex) {
            Notification.show(ex.getMessage(), 4000, Notification.Position.TOP_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    private Optional<User> currentCurator() {
        return authenticationContext.getAuthenticatedUser(UserDetails.class)
                .flatMap(principal -> userService.findByEmail(principal.getUsername()));
    }
}
