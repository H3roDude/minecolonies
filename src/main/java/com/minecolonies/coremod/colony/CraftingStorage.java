package com.minecolonies.coremod.colony;

import net.minecraft.item.Item;

/**
 * @author Isfirs
 */
public class CraftingStorage
{

    private Item[][] input;

    private Item result;

    public CraftingStorage(Item result, Item[][] input)
    {
        this.result = result;
        this.input = input;
    }

    /**
     * TODO change
     *
     * @return
     */
    @Override
    public int hashCode() {
        return super.hashCode();
    }

    /**
     * TODO change
     *
     * @param obj
     * @return
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
        {
            return true;
        }
        if (obj instanceof CraftingStorage)
        {
            CraftingStorage cast = (CraftingStorage) obj;

            // For the moment: just check the outcome to be the same.
            return cast.result == this.result;
        }

        return super.equals(obj);
    }
}
