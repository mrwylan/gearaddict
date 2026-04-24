package app.gearaddict.views.inventory;

import app.gearaddict.KaribuSpringFixture;
import app.gearaddict.PostgresTestContainer;
import app.gearaddict.equipment.Equipment;
import app.gearaddict.equipment.EquipmentCategory;
import app.gearaddict.equipment.EquipmentService;
import app.gearaddict.gear.GearItem;
import app.gearaddict.gear.GearItemFormData;
import app.gearaddict.gear.GearItemService;
import app.gearaddict.user.RegistrationRequest;
import app.gearaddict.user.User;
import app.gearaddict.user.UserRepository;
import app.gearaddict.user.UserService;
import com.github.mvysny.kaributesting.v10.Routes;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.spring.security.AuthenticationContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.ContextConfiguration;

import java.util.List;
import java.util.Optional;

import static com.github.mvysny.kaributesting.v10.LocatorJ._click;
import static com.github.mvysny.kaributesting.v10.LocatorJ._find;
import static com.github.mvysny.kaributesting.v10.LocatorJ._get;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SpringBootTest
@ContextConfiguration(initializers = PostgresTestContainer.class)
class InventoryViewTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private GearItemService gearItemService;

    @Autowired
    private EquipmentService equipmentService;

    @Autowired
    private ApplicationContext applicationContext;

    private final Routes routes = new Routes().autoDiscoverViews("app.gearaddict.views");

    private User alice;
    private AuthenticationContext auth;
    private Equipment minimoog;
    private Equipment prophet;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        alice = userService.register(
                new RegistrationRequest("alice", "alice@example.com", "password123"));
        auth = mock(AuthenticationContext.class);
        UserDetails principal = new org.springframework.security.core.userdetails.User(
                "alice@example.com", "n/a", List.of(new SimpleGrantedAuthority("ROLE_USER")));
        when(auth.getAuthenticatedUser(UserDetails.class)).thenReturn(Optional.of(principal));
        when(auth.getPrincipalName()).thenReturn(Optional.of("alice@example.com"));

        minimoog = catalogEquipmentByName("Minimoog Model D");
        prophet = catalogEquipmentByName("Prophet-5");

        KaribuSpringFixture.setUp(applicationContext, routes);
    }

    @AfterEach
    void tearDown() {
        KaribuSpringFixture.tearDown();
    }

    private InventoryView openView() {
        InventoryView view = new InventoryView(auth, gearItemService, equipmentService, userService);
        UI.getCurrent().add(view);
        return view;
    }

    private Equipment catalogEquipmentByName(String name) {
        return equipmentService
                .browse(Optional.empty(), Optional.of(name), 0, 10)
                .stream()
                .filter(e -> e.name().equals(name))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Seeded equipment missing: " + name));
    }

    private static void fireConfirm(ConfirmDialog dialog) {
        ComponentUtil.fireEvent(dialog, new ConfirmDialog.ConfirmEvent(dialog, true));
    }

    private static void fireCancel(ConfirmDialog dialog) {
        ComponentUtil.fireEvent(dialog, new ConfirmDialog.CancelEvent(dialog, true));
    }

    // ------------------------------------------------------------------
    // UC-009 Add Gear Item
    // ------------------------------------------------------------------

    @Test
    void addButtonOpensEmptyDialog() {
        openView();

        _click(_get(Button.class, spec -> spec.withId("add-gear")));

        TextField deviceName = _get(TextField.class, spec -> spec.withId("gear-form-name"));
        @SuppressWarnings("unchecked")
        Select<EquipmentCategory> category = _get(Select.class, spec -> spec.withId("gear-form-category"));
        TextArea notes = _get(TextArea.class, spec -> spec.withId("gear-form-notes"));

        assertThat(deviceName.getValue()).isEmpty();
        assertThat(category.getValue()).isNull();
        assertThat(notes.getValue()).isEmpty();
    }

    @Test
    void savingCatalogLinkedItemPersistsAndShowsCard() {
        openView();
        _click(_get(Button.class, spec -> spec.withId("add-gear")));

        @SuppressWarnings("unchecked")
        ComboBox<Equipment> equipmentCombo = _get(ComboBox.class, spec -> spec.withId("gear-form-equipment"));
        equipmentCombo.setValue(minimoog);

        @SuppressWarnings("unchecked")
        Select<EquipmentCategory> category = _get(Select.class, spec -> spec.withId("gear-form-category"));
        category.setValue(EquipmentCategory.SYNTH);

        _get(TextArea.class, spec -> spec.withId("gear-form-notes"))
                .setValue("Bass lead workhorse.");

        _click(_get(Button.class, spec -> spec.withId("gear-form-save")));

        List<GearItem> persisted = gearItemService.listForUser(alice.id(), Optional.empty(), 0, 50);
        assertThat(persisted).hasSize(1);
        GearItem item = persisted.get(0);
        assertThat(item.equipmentId()).isEqualTo(minimoog.id());
        assertThat(item.displayName()).isEqualTo("Minimoog Model D");
        assertThat(item.category()).isEqualTo(EquipmentCategory.SYNTH);
        assertThat(item.notes()).isEqualTo("Bass lead workhorse.");

        Div grid = _get(Div.class, spec -> spec.withId("gear-grid"));
        assertThat(grid.getChildren().count()).isEqualTo(1);
    }

    @Test
    void savingFreeTextItemPersistsWithoutCatalogLink() {
        openView();
        _click(_get(Button.class, spec -> spec.withId("add-gear")));

        _get(TextField.class, spec -> spec.withId("gear-form-name"))
                .setValue("Handbuilt Fuzz");

        @SuppressWarnings("unchecked")
        Select<EquipmentCategory> category = _get(Select.class, spec -> spec.withId("gear-form-category"));
        category.setValue(EquipmentCategory.EFFECT);

        _click(_get(Button.class, spec -> spec.withId("gear-form-save")));

        List<GearItem> persisted = gearItemService.listForUser(alice.id(), Optional.empty(), 0, 50);
        assertThat(persisted).hasSize(1);
        GearItem item = persisted.get(0);
        assertThat(item.equipmentId()).isNull();
        assertThat(item.name()).isEqualTo("Handbuilt Fuzz");
        assertThat(item.category()).isEqualTo(EquipmentCategory.EFFECT);
    }

    @Test
    void missingCategoryShowsValidationError() {
        openView();
        _click(_get(Button.class, spec -> spec.withId("add-gear")));

        // Free-text path so the equipment auto-fill does not set a category for us.
        _get(TextField.class, spec -> spec.withId("gear-form-name"))
                .setValue("Mystery pedal");

        _click(_get(Button.class, spec -> spec.withId("gear-form-save")));

        @SuppressWarnings("unchecked")
        Select<EquipmentCategory> category = _get(Select.class, spec -> spec.withId("gear-form-category"));
        assertThat(category.isInvalid()).isTrue();
        assertThat(gearItemService.countForUser(alice.id(), Optional.empty())).isZero();
    }

    @Test
    void selectingEquipmentAutoFillsCategoryWhenEmpty() {
        openView();
        _click(_get(Button.class, spec -> spec.withId("add-gear")));

        @SuppressWarnings("unchecked")
        ComboBox<Equipment> equipmentCombo = _get(ComboBox.class, spec -> spec.withId("gear-form-equipment"));
        equipmentCombo.setValue(minimoog);

        @SuppressWarnings("unchecked")
        Select<EquipmentCategory> category = _get(Select.class, spec -> spec.withId("gear-form-category"));
        assertThat(category.getValue()).isEqualTo(EquipmentCategory.SYNTH);
    }

    @Test
    void missingEquipmentAndNameShowsValidationError() {
        openView();
        _click(_get(Button.class, spec -> spec.withId("add-gear")));

        @SuppressWarnings("unchecked")
        Select<EquipmentCategory> category = _get(Select.class, spec -> spec.withId("gear-form-category"));
        category.setValue(EquipmentCategory.OTHER);

        _click(_get(Button.class, spec -> spec.withId("gear-form-save")));

        TextField deviceName = _get(TextField.class, spec -> spec.withId("gear-form-name"));
        assertThat(deviceName.isInvalid()).isTrue();
        assertThat(gearItemService.countForUser(alice.id(), Optional.empty())).isZero();
    }

    @Test
    void addingDuplicateShowsConfirmDialogAndPersistsOnConfirm() {
        gearItemService.add(alice.id(),
                new GearItemFormData(minimoog.id(), null, EquipmentCategory.SYNTH, "First unit"));

        openView();
        _click(_get(Button.class, spec -> spec.withId("add-gear")));

        @SuppressWarnings("unchecked")
        ComboBox<Equipment> equipmentCombo = _get(ComboBox.class, spec -> spec.withId("gear-form-equipment"));
        equipmentCombo.setValue(minimoog);
        @SuppressWarnings("unchecked")
        Select<EquipmentCategory> category = _get(Select.class, spec -> spec.withId("gear-form-category"));
        category.setValue(EquipmentCategory.SYNTH);

        _click(_get(Button.class, spec -> spec.withId("gear-form-save")));

        ConfirmDialog confirm = _get(ConfirmDialog.class,
                spec -> spec.withId("gear-form-duplicate-confirm"));
        assertThat(confirm.isOpened()).isTrue();

        fireConfirm(confirm);

        assertThat(gearItemService.countForUser(alice.id(), Optional.empty())).isEqualTo(2);
    }

    @Test
    void addingDuplicateCancelledOnConfirmDialogCancelKeepsSingleItem() {
        gearItemService.add(alice.id(),
                new GearItemFormData(minimoog.id(), null, EquipmentCategory.SYNTH, null));

        openView();
        _click(_get(Button.class, spec -> spec.withId("add-gear")));
        @SuppressWarnings("unchecked")
        ComboBox<Equipment> equipmentCombo = _get(ComboBox.class, spec -> spec.withId("gear-form-equipment"));
        equipmentCombo.setValue(minimoog);
        @SuppressWarnings("unchecked")
        Select<EquipmentCategory> category = _get(Select.class, spec -> spec.withId("gear-form-category"));
        category.setValue(EquipmentCategory.SYNTH);
        _click(_get(Button.class, spec -> spec.withId("gear-form-save")));

        ConfirmDialog confirm = _get(ConfirmDialog.class,
                spec -> spec.withId("gear-form-duplicate-confirm"));
        fireCancel(confirm);

        assertThat(gearItemService.countForUser(alice.id(), Optional.empty())).isEqualTo(1);
    }

    // ------------------------------------------------------------------
    // UC-010 Edit Gear Item
    // ------------------------------------------------------------------

    @Test
    void editDialogPrefillsExistingValues() {
        GearItem existing = gearItemService.add(alice.id(),
                new GearItemFormData(minimoog.id(), null, EquipmentCategory.SYNTH, "Serviced 2023."));
        openView();

        _click(_get(Button.class, spec -> spec.withId("edit-gear-" + existing.id())));

        @SuppressWarnings("unchecked")
        ComboBox<Equipment> equipmentCombo = _get(ComboBox.class, spec -> spec.withId("gear-form-equipment"));
        assertThat(equipmentCombo.getValue()).isNotNull();
        assertThat(equipmentCombo.getValue().id()).isEqualTo(minimoog.id());

        @SuppressWarnings("unchecked")
        Select<EquipmentCategory> category = _get(Select.class, spec -> spec.withId("gear-form-category"));
        assertThat(category.getValue()).isEqualTo(EquipmentCategory.SYNTH);

        assertThat(_get(TextArea.class, spec -> spec.withId("gear-form-notes")).getValue())
                .isEqualTo("Serviced 2023.");
    }

    @Test
    void editSavePersistsChanges() {
        GearItem existing = gearItemService.add(alice.id(),
                new GearItemFormData(minimoog.id(), null, EquipmentCategory.SYNTH, "Old notes."));
        openView();

        _click(_get(Button.class, spec -> spec.withId("edit-gear-" + existing.id())));

        @SuppressWarnings("unchecked")
        Select<EquipmentCategory> category = _get(Select.class, spec -> spec.withId("gear-form-category"));
        category.setValue(EquipmentCategory.KEYBOARD);

        _get(TextArea.class, spec -> spec.withId("gear-form-notes"))
                .setValue("Freshly tuned.");

        _click(_get(Button.class, spec -> spec.withId("gear-form-save")));

        GearItem reloaded = gearItemService.findById(existing.id()).orElseThrow();
        assertThat(reloaded.category()).isEqualTo(EquipmentCategory.KEYBOARD);
        assertThat(reloaded.notes()).isEqualTo("Freshly tuned.");
    }

    @Test
    void editFreeTextItemKeepsNullEquipmentAndUpdatesName() {
        GearItem existing = gearItemService.add(alice.id(),
                new GearItemFormData(null, "DIY Eurorack", EquipmentCategory.OTHER, null));
        openView();

        _click(_get(Button.class, spec -> spec.withId("edit-gear-" + existing.id())));

        TextField deviceName = _get(TextField.class, spec -> spec.withId("gear-form-name"));
        assertThat(deviceName.getValue()).isEqualTo("DIY Eurorack");
        deviceName.setValue("DIY Eurorack rev.2");

        _click(_get(Button.class, spec -> spec.withId("gear-form-save")));

        GearItem reloaded = gearItemService.findById(existing.id()).orElseThrow();
        assertThat(reloaded.equipmentId()).isNull();
        assertThat(reloaded.name()).isEqualTo("DIY Eurorack rev.2");
    }

    @Test
    void editCancelDiscardsChanges() {
        GearItem existing = gearItemService.add(alice.id(),
                new GearItemFormData(minimoog.id(), null, EquipmentCategory.SYNTH, "Original"));
        openView();

        _click(_get(Button.class, spec -> spec.withId("edit-gear-" + existing.id())));
        _get(TextArea.class, spec -> spec.withId("gear-form-notes")).setValue("Unsaved draft");
        _click(_get(Button.class, spec -> spec.withId("gear-form-cancel")));

        GearItem reloaded = gearItemService.findById(existing.id()).orElseThrow();
        assertThat(reloaded.notes()).isEqualTo("Original");
    }

    // ------------------------------------------------------------------
    // UC-011 Remove Gear Item
    // ------------------------------------------------------------------

    @Test
    void removeButtonOpensConfirmDialogWithItemName() {
        GearItem existing = gearItemService.add(alice.id(),
                new GearItemFormData(minimoog.id(), null, EquipmentCategory.SYNTH, null));
        openView();

        _click(_get(Button.class, spec -> spec.withId("remove-gear-" + existing.id())));

        ConfirmDialog confirm = _get(ConfirmDialog.class,
                spec -> spec.withId("remove-gear-confirm"));
        assertThat(confirm.isOpened()).isTrue();
        assertThat(confirm.getElement().getProperty("message", ""))
                .contains("Minimoog Model D");
    }

    @Test
    void removeConfirmDeletesItemFromInventory() {
        GearItem toRemove = gearItemService.add(alice.id(),
                new GearItemFormData(minimoog.id(), null, EquipmentCategory.SYNTH, null));
        GearItem toKeep = gearItemService.add(alice.id(),
                new GearItemFormData(prophet.id(), null, EquipmentCategory.SYNTH, null));
        openView();

        _click(_get(Button.class, spec -> spec.withId("remove-gear-" + toRemove.id())));
        ConfirmDialog confirm = _get(ConfirmDialog.class,
                spec -> spec.withId("remove-gear-confirm"));
        fireConfirm(confirm);

        assertThat(gearItemService.findById(toRemove.id())).isEmpty();
        assertThat(gearItemService.findById(toKeep.id())).isPresent();

        Div grid = _get(Div.class, spec -> spec.withId("gear-grid"));
        assertThat(grid.getChildren().count()).isEqualTo(1);
    }

    @Test
    void removeCancelKeepsItem() {
        GearItem existing = gearItemService.add(alice.id(),
                new GearItemFormData(minimoog.id(), null, EquipmentCategory.SYNTH, null));
        openView();

        _click(_get(Button.class, spec -> spec.withId("remove-gear-" + existing.id())));
        ConfirmDialog confirm = _get(ConfirmDialog.class,
                spec -> spec.withId("remove-gear-confirm"));
        fireCancel(confirm);

        assertThat(gearItemService.findById(existing.id())).isPresent();
    }

    @Test
    void removingAllItemsShowsEmptyStateCta() {
        GearItem existing = gearItemService.add(alice.id(),
                new GearItemFormData(minimoog.id(), null, EquipmentCategory.SYNTH, null));
        openView();

        _click(_get(Button.class, spec -> spec.withId("remove-gear-" + existing.id())));
        fireConfirm(_get(ConfirmDialog.class, spec -> spec.withId("remove-gear-confirm")));

        assertThat(_find(Button.class, spec -> spec.withId("add-first-gear")))
                .isNotEmpty();
    }
}
