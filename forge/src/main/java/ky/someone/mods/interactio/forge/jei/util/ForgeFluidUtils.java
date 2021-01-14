package ky.someone.mods.interactio.forge.jei.util;

import me.shedaniel.architectury.hooks.forge.FluidStackHooksForge;
import net.minecraftforge.fluids.FluidStack;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ForgeFluidUtils {

    public static List<FluidStack> forgeStacksOf(Collection<me.shedaniel.architectury.fluid.FluidStack> stacks) {
        List<FluidStack> forgeStacks = new ArrayList<>();
        for (me.shedaniel.architectury.fluid.FluidStack stack : stacks) {
            forgeStacks.add(FluidStackHooksForge.toForge(stack));
        }
        return forgeStacks;
    }

}
