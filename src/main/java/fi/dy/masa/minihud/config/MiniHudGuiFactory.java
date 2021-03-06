package fi.dy.masa.minihud.config;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.common.config.ConfigElement;
import net.minecraftforge.fml.client.DefaultGuiFactory;
import net.minecraftforge.fml.client.config.GuiConfig;
import net.minecraftforge.fml.client.config.IConfigElement;
import fi.dy.masa.minihud.Reference;

public class MiniHudGuiFactory extends DefaultGuiFactory
{
    public MiniHudGuiFactory()
    {
        super(Reference.MOD_ID, getTitle());
    }

    @Override
    public GuiScreen createConfigGui(GuiScreen parent)
    {
        return new GuiConfig(parent, getConfigElements(), Reference.MOD_ID, false, false, getTitle());
    }

    private static List<IConfigElement> getConfigElements()
    {
        List<IConfigElement> configElements = new ArrayList<IConfigElement>();

        configElements.add(new ConfigElement(Configs.config.getCategory(Configs.CATEGORY_DEBUG_HOTKEYS)));
        configElements.add(new ConfigElement(Configs.config.getCategory(Configs.CATEGORY_DEBUG_RENDERER)));
        configElements.add(new ConfigElement(Configs.config.getCategory(Configs.CATEGORY_GENERIC)));
        configElements.add(new ConfigElement(Configs.config.getCategory(Configs.CATEGORY_INFO_TOGGLE)));
        configElements.add(new ConfigElement(Configs.config.getCategory(Configs.CATEGORY_INFO_HOTKEYS)));
        configElements.add(new ConfigElement(Configs.config.getCategory(Configs.CATEGORY_RENDERER_HOTKEYS)));
        configElements.add(new ConfigElement(Configs.config.getCategory(Configs.CATEGORY_INFO_LINE_ORDER)));

        return configElements;
    }

    private static String getTitle()
    {
        return GuiConfig.getAbridgedConfigPath(Configs.configurationFile.toString());
    }
}
