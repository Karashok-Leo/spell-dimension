package net.karashokleo.spelldimension.item.mod_item;

import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.karashokleo.spelldimension.SpellDimensionNetworking;
import net.karashokleo.spelldimension.component.MageComponent;
import net.karashokleo.spelldimension.data.LangData;
import net.karashokleo.spelldimension.misc.Mage;
import net.karashokleo.spelldimension.misc.MageMajor;
import net.karashokleo.spelldimension.util.ParticleUtil;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;
import net.spell_power.api.MagicSchool;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class MageMedalItem extends Item
{
    private static final int COOL_DOWN = 100;

    public MageMedalItem()
    {
        super(new FabricItemSettings().maxCount(1));
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand)
    {
        ItemStack stack = user.getStackInHand(hand);
        Mage mage = Mage.readFromStack(stack);
        if (mage.isInvalid()) return TypedActionResult.fail(stack);
        if (!world.isClient)
        {
            PacketByteBuf buf = PacketByteBufs.create();
            mage.writeToPacket(buf);
            if (mage.isEmpty())
                SpellDimensionNetworking.sendToTrackers(user, SpellDimensionNetworking.CLEAR_PACKET, buf);
            else if (mage.greaterThan(MageComponent.get(user)) &&
                    mage.grade() != MageComponent.get(user).grade())
                SpellDimensionNetworking.sendToTrackers(user, SpellDimensionNetworking.UPGRADE_PACKET, buf);
        }
        MageComponent.set(user, mage);
        user.getItemCooldownManager().set(this, COOL_DOWN);
        ParticleUtil.ringParticleEmit(user, (mage.grade() + 1) * 30, 5, mage.school());
        return TypedActionResult.success(stack);
    }

    @Override
    public Text getName()
    {
        return Text.translatable(LangData.MAGE_MEDAL);
    }

    @Override
    public Text getName(ItemStack stack)
    {
        Mage mage = Mage.readFromStack(stack);
        if (mage.isEmpty()) return Text.translatable(LangData.BLANK_MAGE_MEDAL);
        return mage.getMageTitle(Text.translatable(LangData.MAGE_MEDAL));
    }

    @Override
    public boolean hasGlint(ItemStack stack)
    {
        return Mage.readFromStack(stack).grade() > 0;
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context)
    {
        super.appendTooltip(stack, world, tooltip, context);
        Mage mage = Mage.readFromStack(stack);
        if (mage.isInvalid())
        {
            tooltip.add(Text.translatable(LangData.TOOLTIP_INVALID));
            return;
        }
        tooltip.add(Text.translatable(LangData.TOOLTIP_MEDAL_USE).formatted(Formatting.GRAY));
        tooltip.add(mage.getMageTitle(Text.translatable(LangData.MAGE)));
    }

    public ItemStack getStack(Mage mage)
    {
        ItemStack stack = this.getDefaultStack();
        mage.writeToStack(stack);
        return stack;
    }

    public ItemStack getStack(int grade, @Nullable MagicSchool school, @Nullable MageMajor major)
    {
        ItemStack stack = this.getDefaultStack();
        Mage.writeToStack(stack, grade, school, major);
        return stack;
    }
}