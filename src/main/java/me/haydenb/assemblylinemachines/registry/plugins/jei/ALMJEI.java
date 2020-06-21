package me.haydenb.assemblylinemachines.registry.plugins.jei;

import me.haydenb.assemblylinemachines.AssemblyLineMachines;
import me.haydenb.assemblylinemachines.crafting.AlloyingCrafting;
import me.haydenb.assemblylinemachines.crafting.BathCrafting;
import me.haydenb.assemblylinemachines.crafting.FluidInGroundRecipe;
import me.haydenb.assemblylinemachines.crafting.GrinderCrafting;
import me.haydenb.assemblylinemachines.crafting.PurifierCrafting;
import me.haydenb.assemblylinemachines.registry.ConfigHandler.ConfigHolder;
import me.haydenb.assemblylinemachines.registry.plugins.jei.categories.AlloyingRecipeCategory;
import me.haydenb.assemblylinemachines.registry.plugins.jei.categories.FluidBathRecipeCategory;
import me.haydenb.assemblylinemachines.registry.plugins.jei.categories.FluidGroundCategory;
import me.haydenb.assemblylinemachines.registry.plugins.jei.categories.GrinderRecipeCategory;
import me.haydenb.assemblylinemachines.registry.plugins.jei.categories.PurifierRecipeCategory;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.helpers.IJeiHelpers;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.util.ResourceLocation;

@JeiPlugin
public class ALMJEI implements IModPlugin{
	
	private GrinderRecipeCategory grinderCategory;
	private FluidBathRecipeCategory bathCategory;
	private PurifierRecipeCategory purifierCategory;
	private AlloyingRecipeCategory alloyingCategory;
	private FluidGroundCategory groundCategory;
	
	@Override
	public ResourceLocation getPluginUid() {
		return new ResourceLocation(AssemblyLineMachines.MODID, "alm");
	}
	
	@Override
	public void registerCategories(IRecipeCategoryRegistration registration) {
		if(ConfigHolder.COMMON.jeiSupport.get() == true) {
			IJeiHelpers helpers = registration.getJeiHelpers();
			IGuiHelper guiHelper = helpers.getGuiHelper();
			grinderCategory = new GrinderRecipeCategory(guiHelper);
			bathCategory = new FluidBathRecipeCategory(guiHelper);
			purifierCategory = new PurifierRecipeCategory(guiHelper);
			alloyingCategory = new AlloyingRecipeCategory(guiHelper);
			groundCategory = new FluidGroundCategory(guiHelper);
			
			registration.addRecipeCategories(grinderCategory, bathCategory, purifierCategory, alloyingCategory, groundCategory);
		}
		
		
	}
	
	@Override
	public void registerRecipes(IRecipeRegistration registration) {
		if(ConfigHolder.COMMON.jeiSupport.get() == true) {
			registration.addRecipes(JEIHelper.getRecipes(GrinderCrafting.GRINDER_RECIPE), grinderCategory.getUid());
			registration.addRecipes(JEIHelper.getRecipes(BathCrafting.BATH_RECIPE), bathCategory.getUid());
			registration.addRecipes(JEIHelper.getRecipes(PurifierCrafting.PURIFIER_RECIPE), purifierCategory.getUid());
			registration.addRecipes(JEIHelper.getRecipes(AlloyingCrafting.ALLOYING_RECIPE), alloyingCategory.getUid());
			registration.addRecipes(JEIHelper.getRecipes(FluidInGroundRecipe.FIG_RECIPE), groundCategory.getUid());
		}
	}

}
