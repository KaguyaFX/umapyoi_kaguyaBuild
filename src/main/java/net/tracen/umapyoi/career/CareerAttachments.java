package net.tracen.umapyoi.career;

import java.util.function.Supplier;

import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.tracen.umapyoi.Umapyoi;

public class CareerAttachments {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES = DeferredRegister
            .create(NeoForgeRegistries.ATTACHMENT_TYPES, Umapyoi.MODID);

    /** Not serialized on purpose: the soul is the source of truth. */
    public static final Supplier<AttachmentType<CareerCache>> CAREER_CACHE = ATTACHMENT_TYPES.register("career_cache",
            () -> AttachmentType.builder(() -> new CareerCache()).build());
}
