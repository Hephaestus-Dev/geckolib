/*
 * Copyright (c) 2020.
 * Author: Bernie G. (Gecko)
 */

package software.bernie.geckolib.animation.model;

import com.eliotlash.mclib.math.Variable;
import com.eliotlash.molang.MolangParser;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.model.BipedModel;
import net.minecraft.client.renderer.model.ModelRenderer;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.resources.IReloadableResourceManager;
import net.minecraft.resources.IResourceManager;
import net.minecraft.resources.IResourceManagerReloadListener;
import net.minecraft.util.Util;
import software.bernie.geckolib.animation.builder.Animation;
import software.bernie.geckolib.animation.processor.AnimationProcessor;
import software.bernie.geckolib.animation.processor.IBone;
import software.bernie.geckolib.animation.render.AnimatedModelRenderer;
import software.bernie.geckolib.entity.IAnimatable;
import software.bernie.geckolib.event.predicate.SpecialAnimationPredicate;
import software.bernie.geckolib.file.AnimationFileLoader;
import software.bernie.geckolib.file.IAnimtableModel;
import software.bernie.geckolib.item.armor.AnimatedArmorItem;
import software.bernie.geckolib.manager.AnimationManager;
import software.bernie.geckolib.reload.ReloadManager;

import java.util.ArrayList;
import java.util.List;

/**
 * An AnimatedEntityModel is the equivalent of an Entity Model, except it provides extra functionality for rendering animations from bedrock json animation files. The entity passed into the generic parameter needs to implement IAnimatedEntity.
 *
 * @param <T> the type parameter
 */
public abstract class AnimatedArmorModel<T extends AnimatedArmorItem & IAnimatable> extends BipedModel implements IAnimtableModel, IResourceManagerReloadListener
{
	public List<AnimatedModelRenderer> rootBones = new ArrayList<>();
	public double seekTime;
	public double lastGameTickTime;
	private final AnimationProcessor processor;
	private final AnimationFileLoader loader;
	private final MolangParser parser = new MolangParser();

	private AnimatedModelRenderer helmetRenderer;
	private AnimatedModelRenderer chestplateRenderer;

	private AnimatedModelRenderer leftArmRenderer;
	private AnimatedModelRenderer rightArmRenderer;

	private AnimatedModelRenderer leftLegRenderer;
	private AnimatedModelRenderer rightLegRenderer;

	private AnimatedModelRenderer leftBootRenderer;
	private AnimatedModelRenderer rightBootRenderer;

	private boolean hasSetup = false;


	/**
	 * Instantiates a new Animated entity model and loads the current animation file.
	 */
	protected AnimatedArmorModel()
	{
		super(1);
		ReloadManager.registerModel(this);
		IReloadableResourceManager resourceManager = (IReloadableResourceManager) Minecraft.getInstance().getResourceManager();
		this.processor = new AnimationProcessor();
		this.loader = new AnimationFileLoader(this);
		registerMolangVariables();

		onResourceManagerReload(resourceManager);
	}

	private void registerMolangVariables()
	{
		parser.register(new Variable("query.anim_time", 0));
	}

	/**
	 * Internal method for handling reloads of animation files. Do not override.
	 */
	@Override
	public void onResourceManagerReload(IResourceManager resourceManager)
	{
		this.loader.onResourceManagerReload(resourceManager, parser);
	}

	/**
	 * Gets a bone by name.
	 *
	 * @param boneName The bone name
	 * @return the bone
	 */
	public IBone getBone(String boneName)
	{
		return processor.getBone(boneName);
	}

	/**
	 * Register model renderer. Each AnimatedModelRenderer (group in blockbench) NEEDS to be registered via this method.
	 *
	 * @param modelRenderer The model renderer
	 */
	public void registerModelRenderer(IBone modelRenderer)
	{
		processor.registerModelRenderer(modelRenderer);
	}


	/**
	 * Sets a rotation angle.
	 *
	 * @param modelRenderer The animated model renderer
	 * @param x             x
	 * @param y             y
	 * @param z             z
	 */
	public void setRotationAngle(AnimatedModelRenderer modelRenderer, float x, float y, float z)
	{
		modelRenderer.rotateAngleX = x;
		modelRenderer.rotateAngleY = y;
		modelRenderer.rotateAngleZ = z;
	}

	public void setLivingAnimations(T entity)
	{
		// Each animation has it's own collection of animations (called the EntityAnimationManager), which allows for multiple independent animations
		AnimationManager manager = entity.getAnimationManager();
		if(manager.startTick == null)
		{
			manager.startTick = getCurrentTick();
		}

		manager.tick = (getCurrentTick() - manager.startTick);
		double gameTick = manager.tick;
		double deltaTicks = gameTick - lastGameTickTime;
		seekTime += manager.getCurrentAnimationSpeed() * deltaTicks;
		lastGameTickTime = gameTick;

		SpecialAnimationPredicate<T> predicate = new SpecialAnimationPredicate<T>(entity, seekTime);
		processor.tickAnimation(entity, seekTime, predicate, parser);
	}


	public Animation getAnimation(String name)
	{
		return loader.getAnimation(name);
	}

	@Override
	public void render(MatrixStack matrixStack, IVertexBuilder buffer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha)
	{
		copyModelAngles(this.bipedHead, this.helmetRenderer);
		copyModelAngles(this.bipedBody, this.chestplateRenderer);
		copyModelAngles(this.bipedRightArm, this.rightArmRenderer);
		copyModelAngles(this.bipedLeftArm, this.leftArmRenderer);
		copyModelAngles(this.bipedRightLeg, this.rightLegRenderer);
		copyModelAngles(this.bipedLeftLeg, this.leftLegRenderer);
		copyModelAngles(this.bipedRightLeg, this.rightBootRenderer);
		copyModelAngles(this.bipedLeftLeg, this.leftBootRenderer);

		matrixStack.push();
		if(isSneak) matrixStack.translate(0, 0.2, 0);
		for (AnimatedModelRenderer model : rootBones)
		{
			model.render(matrixStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
		}
		matrixStack.pop();
	}

	/**
	 * If animations should loop by default and ignore their pre-existing loop settings (that you can enable in blockbench by right clicking)
	 */
	public boolean isLoopByDefault()
	{
		return loader.isLoopByDefault();
	}

	/**
	 * If animations should loop by default and ignore their pre-existing loop settings (that you can enable in blockbench by right clicking)
	 */
	public void setLoopByDefault(boolean loopByDefault)
	{
		this.loader.setLoopByDefault(loopByDefault);
	}


	public float getCurrentTick()
	{
		return (Util.milliTime() / 50f);
	}


	private final void copyModelAngles(ModelRenderer in, ModelRenderer out){
		out.rotateAngleX = in.rotateAngleX;
		out.rotateAngleY = in.rotateAngleY;
		out.rotateAngleZ = in.rotateAngleZ;
	}

	public BipedModel applySlot(EquipmentSlotType slot){
		if(!hasSetup)
		{
			setupArmor();
			hasSetup = true;
		}

		helmetRenderer.showModel = false;
		chestplateRenderer.showModel = false;
		rightArmRenderer.showModel = false;
		leftArmRenderer.showModel = false;
		rightLegRenderer.showModel = false;
		leftLegRenderer.showModel = false;
		rightBootRenderer.showModel = false;
		leftBootRenderer.showModel = false;

		switch(slot){
			case HEAD:
				helmetRenderer.showModel = true;
				break;
			case CHEST:
				chestplateRenderer.showModel = true;
				rightArmRenderer.showModel = true;
				leftArmRenderer.showModel = true;
				break;
			case LEGS:
				rightLegRenderer.showModel = true;
				leftLegRenderer.showModel = true;
				break;
			case FEET:
				rightBootRenderer.showModel = true;
				leftBootRenderer.showModel = true;
				break;
			default:
				break;
		}
		return this;
	}

	public void setHelmet(AnimatedModelRenderer helmetRenderer)
	{
		this.helmetRenderer = helmetRenderer;
	}

	public void setChestPlate(AnimatedModelRenderer chestPlate, AnimatedModelRenderer leftArm, AnimatedModelRenderer rightArm)
	{
		this.chestplateRenderer = chestPlate;
		this.leftArmRenderer = leftArm;
		this.rightArmRenderer = rightArm;
	}

	public void setLeggings(AnimatedModelRenderer leftLeg, AnimatedModelRenderer rightLeg)
	{
		this.leftLegRenderer = leftLeg;
		this.rightLegRenderer = rightLeg;
	}

	public void setBoots(AnimatedModelRenderer leftBoot, AnimatedModelRenderer rightBoot)
	{
		this.leftBootRenderer = leftBoot;
		this.rightBootRenderer = rightBoot;
	}

	public abstract void setupArmor();

}