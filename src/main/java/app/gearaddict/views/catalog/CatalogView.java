package app.gearaddict.views.catalog;

import app.gearaddict.equipment.Equipment;
import app.gearaddict.equipment.EquipmentCategory;
import app.gearaddict.equipment.EquipmentService;
import app.gearaddict.views.MainLayout;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
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
    private final Select<EquipmentCategory> categoryFilter = new Select<>();
    private final Grid<Equipment> grid = new Grid<>(Equipment.class, false);

    private Optional<EquipmentCategory> selectedCategory = Optional.empty();

    public CatalogView(EquipmentService equipmentService) {
        this.equipmentService = equipmentService;

        addClassName("catalog-view");
        setSizeFull();
        setPadding(true);

        add(new H1("Equipment Catalog"), buildCategoryFilter(), grid);
        expand(grid);

        configureGrid();
        refreshEmptyState();
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
                query -> equipmentService.browse(selectedCategory, query.getOffset(), query.getLimit()).stream(),
                query -> equipmentService.count(selectedCategory));

        grid.addItemClickListener(event -> UI.getCurrent()
                .navigate("equipment/" + event.getItem().id()));
    }

    private void refreshEmptyState() {
        grid.setEmptyStateText(selectedCategory
                .map(c -> "No equipment in the " + c.label() + " category.")
                .orElse("No equipment in the catalog yet."));
    }
}
