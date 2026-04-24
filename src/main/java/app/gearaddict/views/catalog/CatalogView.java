package app.gearaddict.views.catalog;

import app.gearaddict.equipment.Equipment;
import app.gearaddict.equipment.EquipmentCategory;
import app.gearaddict.equipment.EquipmentService;
import app.gearaddict.views.MainLayout;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Route(value = "catalog", layout = MainLayout.class)
@PageTitle("Equipment Catalog — GearAddict")
@AnonymousAllowed
public class CatalogView extends VerticalLayout {

    public static final String ROUTE = "catalog";

    private final transient EquipmentService equipmentService;
    private final TextField searchField = new TextField();
    private final Select<EquipmentCategory> categoryFilter = new Select<>();
    private final Grid<Equipment> grid = new Grid<>(Equipment.class, false);

    private Optional<EquipmentCategory> selectedCategory = Optional.empty();
    private Optional<String> activeSearchTerm = Optional.empty();

    public CatalogView(EquipmentService equipmentService) {
        this.equipmentService = equipmentService;

        addClassName("catalog-view");
        setSizeFull();
        setPadding(true);

        add(new H1("Equipment Catalog"), buildFilters(), grid);
        expand(grid);

        configureGrid();
        refreshEmptyState();
    }

    private HorizontalLayout buildFilters() {
        HorizontalLayout filters = new HorizontalLayout(buildSearchField(), buildCategoryFilter());
        filters.setWidthFull();
        filters.setAlignItems(Alignment.END);
        return filters;
    }

    private TextField buildSearchField() {
        searchField.setId("search-field");
        searchField.setLabel("Search");
        searchField.setPlaceholder("Search by name or manufacturer...");
        searchField.setClearButtonVisible(true);
        searchField.setPrefixComponent(VaadinIcon.SEARCH.create());
        searchField.setValueChangeMode(ValueChangeMode.LAZY);
        searchField.setValueChangeTimeout(300);
        searchField.setMinLength(0);
        searchField.setWidthFull();
        searchField.addValueChangeListener(event -> onSearchTermChanged(event.getValue()));
        return searchField;
    }

    private Select<EquipmentCategory> buildCategoryFilter() {
        categoryFilter.setId("category-filter");
        categoryFilter.setLabel("Category");
        categoryFilter.setEmptySelectionAllowed(true);
        categoryFilter.setEmptySelectionCaption("All categories");
        List<EquipmentCategory> items = new ArrayList<>(List.of(EquipmentCategory.values()));
        categoryFilter.setItems(items);
        categoryFilter.setItemLabelGenerator(c -> c == null ? "All categories" : c.label());
        categoryFilter.addValueChangeListener(event -> {
            selectedCategory = Optional.ofNullable(event.getValue());
            grid.getLazyDataView().refreshAll();
            refreshEmptyState();
        });
        return categoryFilter;
    }

    private void configureGrid() {
        grid.setId("equipment-grid");
        grid.setSizeFull();
        grid.addColumn(Equipment::name).setHeader("Name").setAutoWidth(true).setFlexGrow(1);
        grid.addColumn(Equipment::manufacturer).setHeader("Manufacturer").setAutoWidth(true).setFlexGrow(1);
        grid.addColumn(equipment -> equipment.category().label()).setHeader("Category").setAutoWidth(true);

        grid.setItems(
                query -> equipmentService
                        .browse(selectedCategory, activeSearchTerm, query.getOffset(), query.getLimit())
                        .stream(),
                query -> equipmentService.count(selectedCategory, activeSearchTerm));

        grid.addItemClickListener(event -> UI.getCurrent()
                .navigate("equipment/" + event.getItem().id()));
    }

    private void onSearchTermChanged(String rawValue) {
        String trimmed = rawValue == null ? "" : rawValue.trim();
        if (trimmed.isEmpty()) {
            searchField.setHelperText(null);
            activeSearchTerm = Optional.empty();
        } else if (trimmed.length() < EquipmentService.MIN_SEARCH_TERM_LENGTH) {
            searchField.setHelperText("Enter at least "
                    + EquipmentService.MIN_SEARCH_TERM_LENGTH + " characters to search.");
            return;
        } else {
            searchField.setHelperText(null);
            activeSearchTerm = Optional.of(trimmed);
        }
        grid.getLazyDataView().refreshAll();
        refreshEmptyState();
    }

    private void refreshEmptyState() {
        if (activeSearchTerm.isPresent()) {
            grid.setEmptyStateText("No results found for \"" + activeSearchTerm.get() + "\".");
            return;
        }
        grid.setEmptyStateText(selectedCategory
                .map(c -> "No equipment in the " + c.label() + " category.")
                .orElse("No equipment in the catalog yet."));
    }
}
