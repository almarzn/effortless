package dev.huskuraft.effortless.screen.item;

import java.util.List;
import java.util.function.Consumer;

import dev.huskuraft.effortless.api.core.Item;
import dev.huskuraft.effortless.api.core.ItemStack;
import dev.huskuraft.effortless.api.gui.AbstractContainerScreen;
import dev.huskuraft.effortless.api.gui.button.Button;
import dev.huskuraft.effortless.api.gui.text.TextWidget;
import dev.huskuraft.effortless.api.platform.Entrance;
import dev.huskuraft.effortless.api.text.ChatFormatting;
import dev.huskuraft.effortless.api.text.Text;

public class EffortlessItemsScreen extends AbstractContainerScreen {

    private final Consumer<List<Item>> consumer;
    private TextWidget titleTextWidget;
    private ItemStackList entries;
    private Button deleteButton;
    private Button clearButton;
    private Button addButton;
    private Button cancelButton;
    private Button saveButton;

    private List<Item> originalItems;
    private List<Item> items;

    public EffortlessItemsScreen(Entrance entrance, Text title, List<Item> items, Consumer<List<Item>> consumer) {
        super(entrance, title, CONTAINER_WIDTH_EXPANDED, CONTAINER_HEIGHT_270);
        this.consumer = consumer;
        this.originalItems = items.stream().distinct().toList();
        this.items = items.stream().distinct().toList();
    }

    @Override
    public void onCreate() {

        this.titleTextWidget = addWidget(new TextWidget(getEntrance(), getLeft() + getWidth() / 2, getTop() + TITLE_CONTAINER - 10, getScreenTitle().withStyle(ChatFormatting.DARK_GRAY), TextWidget.Gravity.CENTER));

        this.entries = addWidget(new ItemStackList(getEntrance(), getLeft() + PADDINGS, getTop() + TITLE_CONTAINER, getWidth() - PADDINGS * 2 - 8, getHeight() - TITLE_CONTAINER - BUTTON_CONTAINER_ROW_2));
        this.entries.setAlwaysShowScrollbar(true);

        this.deleteButton = addWidget(Button.builder(getEntrance(), Text.translate("effortless.button.delete"), button -> {
            if (entries.hasSelected()) {
                entries.deleteSelected();
            }
        }).setBoundsGrid(getLeft(), getTop(), getWidth(), getHeight(), 1f, 0f, 1 / 3f).build());

        this.clearButton = addWidget(Button.builder(getEntrance(), Text.translate("effortless.button.clear"), button -> {
            entries.clear();
        }).setBoundsGrid(getLeft(), getTop(), getWidth(), getHeight(), 1f, 1 / 3f, 1 / 3f).build());
        this.addButton = addWidget(Button.builder(getEntrance(), Text.translate("effortless.button.add"), button -> {
            new EffortlessItemPickerScreen(getEntrance(), (item) -> {
                if (item != null && !items.contains(item)) {
                    entries.insertSelected(item.getDefaultStack());
                    onReload();
                }
            }).attach();
        }).setBoundsGrid(getLeft(), getTop(), getWidth(), getHeight(), 1f, 2 / 3f, 1 / 3f).build());
        this.entries.reset(items.stream().map(Item::getDefaultStack).toList());

        this.cancelButton = addWidget(Button.builder(getEntrance(), Text.translate("effortless.button.cancel"), button -> {
            detach();
        }).setBoundsGrid(getLeft(), getTop(), getWidth(), getHeight(), 0f, 0f, 0.5f).build());

        this.saveButton = addWidget(Button.builder(getEntrance(), Text.translate("effortless.button.save"), button -> {
            consumer.accept(items);
            detach();
        }).setBoundsGrid(getLeft(), getTop(), getWidth(), getHeight(), 0f, 0.5f, 0.5f).build());

    }

    @Override
    public void onReload() {
        this.items = entries.items().stream().map(ItemStack::getItem).toList();
        this.deleteButton.setActive(entries.hasSelected());
        this.clearButton.setActive(!entries.items().isEmpty());
    }

}
