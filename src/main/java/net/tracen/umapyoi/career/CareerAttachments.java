package net.tracen.umapyoi.career;

import java.util.function.Supplier;

import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.tracen.umapyoi.Umapyoi;
import net.tracen.umapyoi.trait.TraitState;

public class CareerAttachments {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES = DeferredRegister
            .create(NeoForgeRegistries.ATTACHMENT_TYPES, Umapyoi.MODID);

    /** Not serialized on purpose: the soul is the source of truth. */
    public static final Supplier<AttachmentType<CareerCache>> CAREER_CACHE = ATTACHMENT_TYPES.register("career_cache",
            () -> AttachmentType.builder(() -> new CareerCache()).build());

    /** Timers for the Uma traits. Not serialized: they restart after a relog, which is harmless. */
    public static final Supplier<AttachmentType<TraitState>> TRAIT_STATE = ATTACHMENT_TYPES.register("trait_state",
            () -> AttachmentType.builder(() -> new TraitState()).build());
}
