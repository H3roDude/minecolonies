package com.minecolonies.coremod.colony;

import com.minecolonies.api.citizen.ICitizenData;
import com.minecolonies.api.citizen.IEntityCitizen;
import com.minecolonies.api.colony.building.IBuilding;
import com.minecolonies.api.configurations.Configurations;
import com.minecolonies.api.job.IJob;
import com.minecolonies.api.util.BlockPosUtil;
import com.minecolonies.api.util.Log;
import com.minecolonies.coremod.colony.buildings.BuildingHome;
import com.minecolonies.coremod.colony.jobs.AbstractJob;
import com.minecolonies.coremod.entity.EntityCitizen;
import com.minecolonies.skeleton.ai.AbstractAISkeleton;
import com.minecolonies.skeleton.colony.building.AbstractBuildingWorker;
import io.netty.buffer.ByteBuf;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Random;

/**
 * Extra data for Citizens.
 */
public class CitizenData implements com.minecolonies.api.citizen.ICitizenData
{
    private static final float  MAX_HEALTH              = 20.0F;
    /**
     * Max level of an attribute a citizen may initially have.
     */
    private static final int    LEVEL_CAP               = 5;
    private static final int    LETTERS_IN_THE_ALPHABET = 26;
    /**
     * Tags.
     */
    private static final String TAG_ID                  = "id";
    private static final String TAG_NAME                = "name";
    private static final String TAG_FEMALE              = "female";
    private static final String TAG_TEXTURE             = "texture";
    private static final String TAG_LEVEL               = "level";
    private static final String TAG_EXPERIENCE          = "experience";
    private static final String TAG_HEALTH              = "health";
    private static final String TAG_MAX_HEALTH          = "maxHealth";
    private static final String TAG_SKILLS              = "skills";
    private static final String TAG_SKILL_STRENGTH      = "strength";
    private static final String TAG_SKILL_STAMINA       = "endurance";
    private static final String TAG_SKILL_SPEED         = "charisma";
    private static final String TAG_SKILL_INTELLIGENCE  = "intelligence";
    private static final String TAG_SKILL_DEXTERITY     = "dexterity";
    /**
     * The unique citizen id.
     */
    private final int                    id;
    private       String                 name;
    private       boolean                female;
    private       int                    textureId;
    private final Colony                 colony;
    @Nullable
    private       IBuilding           homeBuilding;
    @Nullable
    private       AbstractBuildingWorker workBuilding;
    private       IJob            job;
    private       boolean                dirty;
    //Citizen
    @Nullable
    private       IEntityCitizen          entity;
    /**
     * Attributes, which influence the workers behaviour.
     * May be added more later.
     */
    private       int                    strength;
    private       int                    endurance;
    private       int                    charisma;
    private       int                    intelligence;
    private       int                    dexterity;
    private       double                 health;
    private       double                 maxHealth;
    /**
     * The current experience level the citizen is on.
     */
    private int level = 0;

    /**
     * The total amount of experience the citizen has.
     * This also includes the amount of experience within their Experience Bar.
     */
    private double experience;

    /**
     * Create a CitizenData given an ID.
     * Used as a super-constructor or during loading.
     *
     * @param id     ID of the Citizen.
     * @param colony Colony the Citizen belongs to.
     */
    public CitizenData(final int id, final Colony colony)
    {
        this.id = id;
        this.colony = colony;
    }

    /**
     * Creates CitizenData from tag compound.
     *
     * @param compound NBT compound to build from.
     * @param colony   Colony of the citizen.
     * @return CitizenData.
     */
    @NotNull
    public static ICitizenData createFromNBT(@NotNull final NBTTagCompound compound, final Colony colony)
    {
        final int id = compound.getInteger(TAG_ID);
        final @NotNull ICitizenData citizen = new CitizenData(id, colony);
        citizen.readFromNBT(compound);
        return citizen;
    }

    /**
     * Reads data from NBT-tag compound.
     *
     * @param compound NBT-Tag compound.
     */
    @Override
    public void readFromNBT(@NotNull final NBTTagCompound compound)
    {
        name = compound.getString(TAG_NAME);
        female = compound.getBoolean(TAG_FEMALE);
        textureId = compound.getInteger(TAG_TEXTURE);

        //  Attributes
        level = compound.getInteger(TAG_LEVEL);
        experience = compound.getInteger(TAG_EXPERIENCE);
        health = compound.getFloat(TAG_HEALTH);
        maxHealth = compound.getFloat(TAG_MAX_HEALTH);


        final NBTTagCompound nbtTagSkillsCompound = compound.getCompoundTag("skills");
        strength = nbtTagSkillsCompound.getInteger("strength");
        endurance = nbtTagSkillsCompound.getInteger("endurance");
        charisma = nbtTagSkillsCompound.getInteger("charisma");
        intelligence = nbtTagSkillsCompound.getInteger("intelligence");
        dexterity = nbtTagSkillsCompound.getInteger("dexterity");

        if (compound.hasKey("job"))
        {
            setJob(AbstractJob.createFromNBT(this, compound.getCompoundTag("job")));
        }
    }

    /**
     * Return the entity instance of the citizen data. Respawn the citizen if needed.
     *
     * @return {@link EntityCitizen} of the citizen data.
     */
    @Override
    @Nullable
    public IEntityCitizen getCitizenEntity()
    {
        return entity;
    }

    /**
     * Sets the entity of the citizen data.
     *
     * @param citizen {@link EntityCitizen} instance of the citizen data.
     */
    @Override
    public void setCitizenEntity(final IEntityCitizen citizen)
    {
        entity = citizen;
        markDirty();
    }

    /**
     * Marks the instance dirty.
     */
    @Override
    public void markDirty()
    {
        dirty = true;
        colony.markCitizensDirty();
    }

    /**
     * Create a CitizenData View given it's saved NBTTagCompound.
     *
     * @param id  The citizen's id.
     * @param buf The network data.
     * @return View object of the citizen.
     */
    @Nullable
    public static CitizenDataView createCitizenDataView(final int id, final ByteBuf buf)
    {
        @Nullable CitizenDataView citizenDataView = new CitizenDataView(id);

        try
        {
            citizenDataView.deserialize(buf);
        }
        catch (final RuntimeException ex)
        {
            Log.getLogger().error(String.format("A CitizenData.View for #%d has thrown an exception during loading, its state cannot be restored. Report this to the mod author",
              citizenDataView.getID()), ex);
            citizenDataView = null;
        }

        return citizenDataView;
    }

    /**
     * Create a CitizenData given a CitizenEntity.
     *
     * @param entity Entity to initialize from.
     */
    @Override
    public void initializeFromEntity(@NotNull final IEntityCitizen entity)
    {
        final Random rand = entity.getRandom();

        this.entity = entity;

        //Assign the gender before name
        female = rand.nextBoolean();
        name = generateName(rand);

        textureId = entity.getEntityWorld().rand.nextInt(Integer.MAX_VALUE);
        health = entity.getHealth();
        maxHealth = entity.getMaxHealth();
        experience = 0;
        level = 0;
        @NotNull final Random random = new Random();

        //Initialize the citizen skills and make sure they are never 0
        intelligence = random.nextInt(LEVEL_CAP - 1) + 1;
        charisma = random.nextInt(LEVEL_CAP - 1) + 1;
        strength = random.nextInt(LEVEL_CAP - 1) + 1;
        endurance = random.nextInt(LEVEL_CAP - 1) + 1;
        dexterity = random.nextInt(LEVEL_CAP - 1) + 1;

        markDirty();
    }

    /**
     * Generates a random name from a set of names.
     *
     * @param rand Random object.
     * @return Name of the citizen.
     */
    private String generateName(@NotNull final Random rand)
    {
        String citizenName;
        if (female)
        {
            citizenName = String.format("%s %s. %s", getRandomElement(rand, Configurations.femaleFirstNames), getRandomLetter(rand), getRandomElement(rand, Configurations.lastNames));
        }
        else
        {
            citizenName = String.format("%s %s. %s", getRandomElement(rand, Configurations.maleFirstNames), getRandomLetter(rand), getRandomElement(rand, Configurations.lastNames));
        }
        for (int i = 1; i <= this.getColony().getMaxCitizens(); i++)
        {
            if (this.getColony().getCitizen(i) != null && this.getColony().getCitizen(i).getName().equals(citizenName))
            {
                citizenName = generateName(rand);
            }
        }
        return citizenName;
    }

    /**
     * Returns a random element in a list.
     *
     * @param rand  Random object.
     * @param array Array to select from.
     * @return Random element from array.
     */
    private static String getRandomElement(@NotNull final Random rand, @NotNull final String[] array)
    {
        return array[rand.nextInt(array.length)];
    }

    /**
     * Returns a random capital letter from the alphabet.
     *
     * @param rand Random object.
     * @return Random capital letter.
     */
    private static char getRandomLetter(@NotNull final Random rand)
    {
        return (char) (rand.nextInt(LETTERS_IN_THE_ALPHABET) + 'A');
    }

    /**
     * Returns the id of the citizen.
     *
     * @return id of the citizen.
     */
    @Override
    public int getId()
    {
        return id;
    }

    /**
     * Returns the colony of the citizen.
     *
     * @return colony of the citizen.
     */
    @Override
    public Colony getColony()
    {
        return colony;
    }

    /**
     * Returns the name of the citizen.
     *
     * @return name of the citizen.
     */
    @Override
    public String getName()
    {
        return name;
    }

    /**
     * Returns true if citizen is female, false for male.
     *
     * @return true for female, false for male.
     */
    @Override
    public boolean isFemale()
    {
        return female;
    }

    /**
     * Returns the texture id for the citizen.
     *
     * @return texture ID.
     */
    @Override
    public int getTextureId()
    {
        return textureId;
    }

    /**
     * Adds experience of the citizen.
     *
     * @param xp the amount of xp to add.
     */
    @Override
    public void addExperience(final double xp)
    {
        this.experience += xp;
    }

    /**
     * Sets the level of the citizen.
     */
    @Override
    public void increaseLevel()
    {
        this.level += 1;
    }

    /**
     * Returns whether or not the instance is dirty.
     *
     * @return true when dirty, otherwise false.
     */
    @Override
    public boolean isDirty()
    {
        return dirty;
    }

    /**
     * Markt the instance not dirty.
     */
    @Override
    public void clearDirty()
    {
        dirty = false;
    }

    /**
     * When a building is destroyed, inform the citizen so it can do any cleanup of associations that the building's.
     * own AbstractBuilding.onDestroyed did not do.
     *
     * @param building building that is destroyed.
     */
    @Override
    public void onRemoveBuilding(final IBuilding building)
    {
        if (getHomeBuilding() == building)
        {
            setHomeBuilding(null);
        }

        if (getWorkBuilding() == building)
        {
            setWorkBuilding(null);
        }
    }

    /**
     * Returns the home building of the citizen.
     *
     * @return home building.
     */
    @Override
    @Nullable
    public IBuilding getHomeBuilding()
    {
        return homeBuilding;
    }

    /**
     * Sets the home of the citizen.
     *
     * @param building home building.
     */
    @Override
    public void setHomeBuilding(@Nullable final IBuilding building)
    {
        if (homeBuilding != null && building != null && homeBuilding != building)
        {
            throw new IllegalStateException("CitizenData.setHomeBuilding() - already assigned a home building when setting a new home building");
        }
        else if (homeBuilding != building)
        {
            homeBuilding = building;
            markDirty();
        }
    }

    /**
     * Returns the work building of a citizen.
     *
     * @return home building of a citizen.
     */
    @Override
    @Nullable
    public AbstractBuildingWorker getWorkBuilding()
    {
        return workBuilding;
    }

    /**
     * Sets the work building of a citizen.
     *
     * @param building work building.
     */
    @Override
    public void setWorkBuilding(@Nullable final AbstractBuildingWorker building)
    {
        if (workBuilding != null && building != null && workBuilding != building)
        {
            throw new IllegalStateException("CitizenData.setWorkBuilding() - already assigned a work building when setting a new work building");
        }
        else if (workBuilding != building)
        {
            workBuilding = building;

            if (workBuilding != null)
            {
                //  We have a place to work, do we have the assigned Job?
                if (job == null)
                {
                    //  No job, create one!
                    setJob(workBuilding.createJob(this));
                    colony.getWorkManager().clearWorkForCitizen(this);
                }
            }
            else if (job != null)
            {
                final IEntityCitizen citizen = getCitizenEntity();
                if (citizen != null)
                {
                    citizen.getTasks().removeTask(citizen.getTasks().taskEntries.stream().filter(task -> task.action instanceof AbstractAISkeleton).findFirst().orElse(null).action);
                }
                //  No place of employment, get rid of our job
                setJob(null);
                colony.getWorkManager().clearWorkForCitizen(this);
            }

            markDirty();
        }
    }

    /**
     * Sets {@link EntityCitizen} to null for the instance.
     */
    @Override
    public void clearCitizenEntity()
    {
        entity = null;
    }

    /**
     * Returns the job of the citizen.
     *
     * @return Job of the citizen.
     */
    @Override
    public IJob getJob()
    {
        return job;
    }

    /**
     * Sets the job of this citizen.
     *
     * @param job Job of the citizen.
     */
    @Override
    public void setJob(final IJob job)
    {
        this.job = job;

        @Nullable final IEntityCitizen localEntity = getCitizenEntity();
        if (localEntity != null)
        {
            localEntity.onJobChanged(job);
        }

        markDirty();
    }

    /**
     * Returns the job subclass needed. Returns null on type mismatch.
     *
     * @param type the type of job wanted.
     * @param <J>  The job type returned.
     * @return the job this citizen has.
     */
    @Override
    @Nullable
    public <J extends IJob> J getJob(@NotNull final Class<J> type)
    {
        try
        {
            return type.cast(job);
        }
        catch (final ClassCastException ignored)
        {
            return null;
        }
    }

    /**
     * Writes the citiizen data to an NBT-compound.
     *
     * @param compound NBT-Tag compound.
     */
    @Override
    public void writeToNBT(@NotNull final NBTTagCompound compound)
    {
        compound.setInteger(TAG_ID, id);
        compound.setString(TAG_NAME, name);
        compound.setBoolean(TAG_FEMALE, female);
        compound.setInteger(TAG_TEXTURE, textureId);

        //  Attributes
        compound.setInteger(TAG_LEVEL, level);
        compound.setDouble(TAG_EXPERIENCE, experience);
        compound.setDouble(TAG_HEALTH, health);
        compound.setDouble(TAG_MAX_HEALTH, maxHealth);


        @NotNull final NBTTagCompound nbtTagSkillsCompound = new NBTTagCompound();
        nbtTagSkillsCompound.setInteger(TAG_SKILL_STRENGTH, strength);
        nbtTagSkillsCompound.setInteger(TAG_SKILL_STAMINA, endurance);
        nbtTagSkillsCompound.setInteger(TAG_SKILL_SPEED, charisma);
        nbtTagSkillsCompound.setInteger(TAG_SKILL_INTELLIGENCE, intelligence);
        nbtTagSkillsCompound.setInteger(TAG_SKILL_DEXTERITY, dexterity);
        compound.setTag(TAG_SKILLS, nbtTagSkillsCompound);

        if (job != null)
        {
            @NotNull final NBTTagCompound jobCompound = new NBTTagCompound();
            job.writeToNBT(jobCompound);
            compound.setTag("job", jobCompound);
        }
    }

    /**
     * Writes the citizen data to a byte buf for transition.
     *
     * @param buf Buffer to write to.
     */
    @Override
    public void serializeViewNetworkData(@NotNull final ByteBuf buf)
    {
        ByteBufUtils.writeUTF8String(buf, name);
        buf.writeBoolean(female);

        buf.writeInt(entity != null ? entity.getEntityId() : -1);

        buf.writeBoolean(homeBuilding != null);
        if (homeBuilding != null)
        {
            BlockPosUtil.writeToByteBuf(buf, homeBuilding.getID());
        }

        buf.writeBoolean(workBuilding != null);
        if (workBuilding != null)
        {
            BlockPosUtil.writeToByteBuf(buf, workBuilding.getID());
        }

        //  Attributes
        buf.writeInt(getLevel());
        buf.writeDouble(getExperience());

        //If entity is null assume the standard values as health
        if (entity == null)
        {
            buf.writeFloat(MAX_HEALTH);
            buf.writeFloat(MAX_HEALTH);
        }
        else
        {
            buf.writeFloat(entity.getHealth());
            buf.writeFloat(entity.getMaxHealth());
        }

        buf.writeInt(getStrength());
        buf.writeInt(getEndurance());
        buf.writeInt(getCharisma());
        buf.writeInt(getIntelligence());
        buf.writeInt(getDexterity());

        ByteBufUtils.writeUTF8String(buf, (job != null) ? job.getName() : "");
    }

    /**
     * Returns the level of the citizen.
     *
     * @return level of the citizen.
     */
    @Override
    public int getLevel()
    {
        return level;
    }

    /**
     * Sets the level of the citizen.
     *
     * @param lvl the new level for the citizen.
     */
    @Override
    public void setLevel(final int lvl)
    {
        this.level = lvl;
    }

    /**
     * Resets the experience and the experience level of the citizen.
     */
    @Override
    public void resetExperienceAndLevel()
    {
        this.level = 0;
        this.experience = 0;
    }

    /**
     * Returns the experience of the citizen.
     *
     * @return experience of the citizen.
     */
    @Override
    public double getExperience()
    {
        return experience;
    }

    /**
     * Strength getter.
     *
     * @return citizen Strength value.
     */
    @Override
    public int getStrength()
    {
        return strength;
    }

    /**
     * Endurance getter.
     *
     * @return citizen Endurance value.
     */
    @Override
    public int getEndurance()
    {
        return endurance;
    }

    /**
     * Charisma getter.
     *
     * @return citizen Charisma value.
     */
    @Override
    public int getCharisma()
    {
        return charisma;
    }

    /**
     * Intelligence getter.
     *
     * @return citizen Intelligence value.
     */
    @Override
    public int getIntelligence()
    {
        return intelligence;
    }

    /**
     * Dexterity getter.
     *
     * @return citizen Dexterity value.
     */
    @Override
    public int getDexterity()
    {
        return dexterity;
    }
}
