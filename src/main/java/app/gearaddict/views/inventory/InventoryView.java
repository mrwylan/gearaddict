package app.gearaddict.views.inventory;

import app.gearaddict.equipment.EquipmentCategory;
import app.gearaddict.equipment.EquipmentService;
import app.gearaddict.gear.GearItem;
import app.gearaddict.gear.GearItemException;
import app.gearaddict.gear.GearItemService;
import app.gearaddict.user.User;
import app.gearaddict.user.UserService;
import app.gearaddict.views.MainLayout;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.security.AuthenticationContext;
import jakarta.annotation.security.PermitAll;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Route(value = "", layout = MainLayout.class)
@PageTitle("My gear — GearAddict")
@PermitAll
public class InventoryView extends VerticalLayout {

    public static final String ROUTE = "";
    private static final int PAGE_SIZE = 60;

    private final transient AuthenticationContext authenticationContext;
    private final transient GearItemService gearItemService;
    private final transient EquipmentService equipmentService;
    private final transient UserService userService;

    private final Select<EquipmentCategory> categoryFilter = new Select<>();
    private final Div gearGrid = new Div();
    private final Div emptyState = new Div();
    private final Button addButton = new Button("Add Gear Item", VaadinIcon.PLUS.create());

    private Optional<EquipmentCategory> selectedCategory = Optional.empty();
    private Long currentUserId;

    public InventoryView(AuthenticationContext authenticationContext,
                         GearItemService gearItemService,
                         EquipmentService equipmentService,
                         UserService userService) {
        this.authenticationContext = authenticationContext;
        this.gearItemService = gearItemService;
        this.equipmentService = equipmentService;
        this.userService = userService;

        addClassName("inventory-view");
        setSizeFull();
        setPadding(true);

        currentUserId = loadCurrentUser().map(User::id).orElse(null);

        add(buildHeader(), buildToolbar(), gearGrid, emptyState);
        configureGrid();

        refresh();
    }

    private HorizontalLayout buildHeader() {
        H1 title = new H1("My Inventory");
        title.getStyle().set("margin", "0");

        addButton.setId("add-gear");
        addButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        addButton.addClickListener(e -> openAddDialog());
        addButton.setEnabled(currentUserId != null);

        HorizontalLayout header = new HorizontalLayout(title, addButton);
        header.setWidthFull();
        header.setAlignItems(FlexComponent.Alignment.CENTER);
        header.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        return header;
    }

    private HorizontalLayout buildToolbar() {
        categoryFilter.setId("category-filter");
        categoryFilter.setLabel("Filter by category");
        categoryFilter.setEmptySelectionAllowed(true);
        categoryFilter.setEmptySelectionCaption("All categories");
        List<EquipmentCategory> items = new ArrayList<>(List.of(EquipmentCategory.values()));
        categoryFilter.setItems(items);
        categoryFilter.setItemLabelGenerator(c -> c == null ? "All categories" : c.label());
        categoryFilter.addValueChangeListener(event -> {
            selectedCategory = Optional.ofNullable(event.getValue());
            refresh();
        });

        HorizontalLayout toolbar = new HorizontalLayout(categoryFilter);
        toolbar.setWidthFull();
        return toolbar;
    }

    private void configureGrid() {
        gearGrid.setId("gear-grid");
        gearGrid.getStyle().set("display", "grid");
        gearGrid.getStyle().set("grid-template-columns", "repeat(auto-fill, minmax(260px, 1fr))");
        gearGrid.getStyle().set("gap", "var(--lumo-space-m)");
        gearGrid.setWidthFull();

        emptyState.setId("empty-state");
        emptyState.getStyle().set("text-align", "center");
        emptyState.getStyle().set("padding", "var(--lumo-space-xl)");
        emptyState.getStyle().set("color", "var(--lumo-secondary-text-color)");
        emptyState.setVisible(false);
    }

    private void refresh() {
        gearGrid.removeAll();
        emptyState.removeAll();
        emptyState.setVisible(false);

        if (currentUserId == null) {
            gearGrid.setVisible(false);
            addButton.setEnabled(false);
            emptyState.add(new Paragraph("You are not signed in."));
            emptyState.setVisible(true);
            return;
        }

        List<GearItem> items = gearItemService.listForUser(currentUserId, selectedCategory, 0, PAGE_SIZE);
        if (items.isEmpty()) {
            gearGrid.setVisible(false);
            emptyState.setVisible(true);
            emptyState.add(buildEmptyState());
            return;
        }

        gearGrid.setVisible(true);
        items.forEach(item -> gearGrid.add(buildCard(item)));
    }

    private Component buildEmptyState() {
        VerticalLayout layout = new VerticalLayout();
        layout.setAlignItems(FlexComponent.Alignment.CENTER);
        layout.setPadding(false);

        if (selectedCategory.isPresent()) {
            layout.add(new Paragraph("No gear items in the "
                    + selectedCategory.get().label() + " category."));
            Button clear = new Button("Clear filter", e -> categoryFilter.clear());
            clear.setId("clear-filter");
            layout.add(clear);
            return layout;
        }

        layout.add(new Paragraph("You haven't added any gear yet."));
        Button addFirst = new Button("Add your first item", e -> openAddDialog());
        addFirst.setId("add-first-gear");
        addFirst.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        layout.add(addFirst);
        return layout;
    }

    private Component buildCard(GearItem item) {
        Div card = new Div();
        card.addClassName("gear-card");
        card.getElement().setAttribute("data-gear-item-id", String.valueOf(item.id()));
        card.getStyle().set("background", "var(--lumo-base-color)");
        card.getStyle().set("border-radius", "var(--lumo-border-radius-l)");
        card.getStyle().set("box-shadow", "var(--lumo-box-shadow-xs)");
        card.getStyle().set("padding", "var(--lumo-space-m)");

        H3 name = new H3(item.displayName());
        name.addClassName("gear-name");
        name.getStyle().set("margin", "0");
        name.getStyle().set("font-size", "var(--lumo-font-size-m)");

        Span info = new Span();
        info.addClassName("gear-info");
        String manufacturer = item.displayManufacturer();
        info.setText(manufacturer == null ? "Not in catalog" : manufacturer);
        info.getStyle().set("color", "var(--lumo-secondary-text-color)");
        info.getStyle().set("font-size", "var(--lumo-font-size-s)");

        Span category = new Span(item.category().label());
        category.addClassName("gear-category");
        category.getElement().getThemeList().add("badge");

        Button edit = new Button(VaadinIcon.EDIT.create(), e -> openEditDialog(item));
        edit.setId("edit-gear-" + item.id());
        edit.addThemeVariants(ButtonVariant.LUMO_ICON, ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
        edit.getElement().setAttribute("aria-label", "Edit " + item.displayName());
        edit.getElement().setAttribute("title", "Edit");

        Button remove = new Button(VaadinIcon.TRASH.create(), e -> confirmRemove(item));
        remove.setId("remove-gear-" + item.id());
        remove.addThemeVariants(ButtonVariant.LUMO_ICON, ButtonVariant.LUMO_TERTIARY,
                ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_ERROR);
        remove.getElement().setAttribute("aria-label", "Remove " + item.displayName());
        remove.getElement().setAttribute("title", "Remove");

        HorizontalLayout actions = new HorizontalLayout(edit, remove);
        actions.setSpacing(false);

        HorizontalLayout header = new HorizontalLayout();
        header.setWidthFull();
        header.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        header.setAlignItems(FlexComponent.Alignment.START);

        VerticalLayout infoColumn = new VerticalLayout(name, info, category);
        infoColumn.setPadding(false);
        infoColumn.setSpacing(false);

        header.add(infoColumn, actions);
        header.expand(infoColumn);

        card.add(header);

        if (item.notes() != null && !item.notes().isBlank()) {
            Paragraph notes = new Paragraph(item.notes());
            notes.addClassName("gear-notes");
            notes.getStyle().set("margin-top", "var(--lumo-space-s)");
            notes.getStyle().set("padding-top", "var(--lumo-space-s)");
            notes.getStyle().set("border-top", "1px solid var(--lumo-contrast-5pct)");
            notes.getStyle().set("color", "var(--lumo-secondary-text-color)");
            notes.getStyle().set("font-size", "var(--lumo-font-size-s)");
            card.add(notes);
        }

        return card;
    }

    private void openAddDialog() {
        if (currentUserId == null) {
            return;
        }
        GearItemFormDialog dialog = new GearItemFormDialog(
                gearItemService, equipmentService, currentUserId, this::refresh);
        dialog.openForAdd();
    }

    private void openEditDialog(GearItem item) {
        if (currentUserId == null) {
            return;
        }
        GearItemFormDialog dialog = new GearItemFormDialog(
                gearItemService, equipmentService, currentUserId, this::refresh);
        dialog.openForEdit(item);
    }

    private void confirmRemove(GearItem item) {
        if (currentUserId == null) {
            return;
        }
        ConfirmDialog confirm = new ConfirmDialog();
        confirm.setId("remove-gear-confirm");
        confirm.setHeader("Remove gear item");
        confirm.setText("Are you sure you want to remove \""
                + item.displayName() + "\" from your inventory?");
        confirm.setCancelable(true);
        confirm.setCancelText("Cancel");
        confirm.setConfirmText("Remove");
        confirm.setConfirmButtonTheme("error primary");
        confirm.addConfirmListener(e -> removeItem(item));
        confirm.open();
    }

    private void removeItem(GearItem item) {
        try {
            gearItemService.remove(currentUserId, item.id());
            Notification.show("Gear item removed.", 3000, Notification.Position.TOP_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            refresh();
        } catch (GearItemException ex) {
            Notification.show(ex.getMessage(), 4000, Notification.Position.TOP_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    private Optional<User> loadCurrentUser() {
        return authenticationContext.getAuthenticatedUser(UserDetails.class)
                .flatMap(principal -> userService.findByEmail(principal.getUsername()));
    }
}
