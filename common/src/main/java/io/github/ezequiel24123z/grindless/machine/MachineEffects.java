package io.github.ezequiel24123z.grindless.machine;

import io.github.ezequiel24123z.grindless.registry.BlockCatalogue.Geometry;
import io.github.ezequiel24123z.grindless.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;

/**
 * What a machine looks and sounds like while in each status, beyond its texture.
 *
 * <p>Runs on the client only, from {@code Block#animateTick}, which the game calls for blocks near
 * the player. Nothing here touches the world's state, and none of it exists on a dedicated server
 * beyond the class itself, so effects cost a server nothing.
 *
 * <p>Each kind of machine has its own signature so they are told apart by ear and by eye before
 * they are read: sparks at a crank, smoke from a stack, glyphs off a screen, a rising glow from a
 * pylon. A fault is always a different <em>kind</em> of effect from normal running, not merely
 * more of it, so it can be noticed from across a room.
 */
public final class MachineEffects {

    private MachineEffects() {
    }

    public static void animate(Geometry geometry, int tier, MachineStatus status, Direction front,
                               Level level, BlockPos pos, RandomSource random) {
        if (status == MachineStatus.IDLE) {
            return;
        }
        switch (geometry) {
            case DYNAMO -> dynamo(status, front, level, pos, random);
            case EXTRACTOR -> extractor(status, level, pos, random);
            case TERMINAL -> terminal(status, front, level, pos, random);
            case GENERATOR -> generator(status, level, pos, random);
            case MILL -> mill(status, level, pos, random);
            case FURNACE -> furnace(status, level, pos, random);
            case PRESS -> mill(status, level, pos, random);
            case ASSEMBLER -> manipulator(status, front, level, pos, random);
            case KILN -> generator(status, level, pos, random);
            case WIRE_MILL -> mill(status, level, pos, random);
            case REACTOR -> mill(status, level, pos, random);
            case WASHER -> washer(status, level, pos, random);
            case CELL -> cell(status, level, pos, random);
            case INTAKE -> intake(status, level, pos, random);
            case WELL -> well(status, level, pos, random);
            case INDUCTION -> induction(status, level, pos, random);
            case CASTER -> caster(status, level, pos, random);
            case FLOTATION -> flotation(status, level, pos, random);
            case MAGNET -> belt(status, level, pos, random);
            case SOLAR -> dynamo(status, front, level, pos, random);
            case BOILER -> generator(status, level, pos, random);
            case CONDENSER -> intake(status, level, pos, random);
            case PYLON -> pylon(tier, status, level, pos, random);
            case BELT -> belt(status, level, pos, random);
            case SPLITTER -> belt(status, level, pos, random);
            case MERGER -> belt(status, level, pos, random);
            case TUNNEL -> belt(status, level, pos, random);
            case OVERFLOW -> belt(status, level, pos, random);
            case SORTER -> belt(status, level, pos, random);
            case MANIPULATOR -> manipulator(status, front, level, pos, random);
            case DRILL -> extractor(status, level, pos, random);
            case CONDUIT -> belt(status, level, pos, random);
            case PRESSURE -> belt(status, level, pos, random);
            case PUMP -> mill(status, level, pos, random);
            case EPUMP -> mill(status, level, pos, random);
            case TANK -> generator(status, level, pos, random);
            case INDUSTRIAL -> generator(status, level, pos, random);
            case FLUID_ARM -> manipulator(status, front, level, pos, random);
            case FLUX_BELT -> belt(status, level, pos, random);
            case STACK_ARM -> manipulator(status, front, level, pos, random);
            case FILTER_ARM -> manipulator(status, front, level, pos, random);
            case SIGNAL -> belt(status, level, pos, random);
            case LOGIC -> terminal(status, front, level, pos, random);
            case INTERFACE -> dynamo(status, front, level, pos, random);
            case SCANNER -> terminal(status, front, level, pos, random);
            case DECONSTRUCTOR -> mill(status, level, pos, random);
            case BANK -> pylon(1, status, level, pos, random);
            case TRANSFORMER -> dynamo(status, front, level, pos, random);
        }
        if (status == MachineStatus.OUT_OF_BAND) {
            outOfBand(level, pos, random);
        }
    }

    /** Froth while the cell runs; a dry click when surfactant or feed is missing. */
    private static void flotation(MachineStatus status, Level level, BlockPos pos, RandomSource random) {
        double x = pos.getX() + 0.5;
        double z = pos.getZ() + 0.5;
        switch (status) {
            case RUNNING -> {
                if (random.nextFloat() < 0.6F) {
                    particle(level, ParticleTypes.BUBBLE_POP, x, pos.getY() + 0.95, z,
                            (random.nextDouble() - 0.5) * 0.15, 0.04, (random.nextDouble() - 0.5) * 0.15);
                }
                sound(level, pos, ModSounds.MACHINE_HUM.get(), random, 0.04F, 0.14F, 0.9F);
            }
            case BLOCKED -> {
                if (random.nextFloat() < 0.2F) {
                    particle(level, ParticleTypes.BUBBLE_POP, x, pos.getY() + 1.0, z, 0.0, 0.01, 0.0);
                }
            }
            case STARVED -> sound(level, pos, ModSounds.RELAY_CLICK.get(), random, 0.03F, 0.12F, 0.8F);
            case OUT_OF_BAND -> {
            }
            default -> {
            }
        }
    }

    /** Spray off the sluice while it runs; a dry click when it is waiting on water or feed. */
    private static void washer(MachineStatus status, Level level, BlockPos pos, RandomSource random) {
        double x = pos.getX() + 0.5;
        double z = pos.getZ() + 0.5;
        switch (status) {
            case RUNNING -> {
                if (random.nextFloat() < 0.7F) {
                    particle(level, ParticleTypes.SPLASH, x, pos.getY() + 0.85, z,
                            (random.nextDouble() - 0.5) * 0.2, 0.05, (random.nextDouble() - 0.5) * 0.2);
                }
                sound(level, pos, ModSounds.MACHINE_HUM.get(), random, 0.04F, 0.15F, 1.2F);
            }
            case BLOCKED -> {
                if (random.nextFloat() < 0.25F) {
                    particle(level, ParticleTypes.SPLASH, x, pos.getY() + 1.0, z, 0.0, 0.02, 0.0);
                }
            }
            case STARVED -> {
                if (random.nextFloat() < 0.15F) {
                    particle(level, ParticleTypes.SMOKE, x, pos.getY() + 0.9, z, 0.0, 0.02, 0.0);
                }
                sound(level, pos, ModSounds.RELAY_CLICK.get(), random, 0.04F, 0.2F, 0.7F);
            }
            case OUT_OF_BAND -> {
            }
            default -> {
            }
        }
    }

    /** Bubbles while current runs; a dry spark when the cell is waiting on water or power. */
    private static void cell(MachineStatus status, Level level, BlockPos pos, RandomSource random) {
        double x = pos.getX() + 0.5;
        double z = pos.getZ() + 0.5;
        switch (status) {
            case RUNNING -> {
                if (random.nextFloat() < 0.6F) {
                    particle(level, ParticleTypes.BUBBLE_POP, x, pos.getY() + 0.9, z,
                            (random.nextDouble() - 0.5) * 0.05, 0.04, (random.nextDouble() - 0.5) * 0.05);
                }
                sound(level, pos, ModSounds.MACHINE_HUM.get(), random, 0.05F, 0.12F, 1.6F);
            }
            case BLOCKED -> {
                if (random.nextFloat() < 0.2F) {
                    particle(level, ParticleTypes.BUBBLE_POP, x, pos.getY() + 1.0, z, 0.0, 0.01, 0.0);
                }
            }
            case STARVED -> {
                if (random.nextFloat() < 0.2F) {
                    particle(level, ParticleTypes.ELECTRIC_SPARK, x, pos.getY() + 0.7, z, 0.0, 0.0, 0.0);
                }
                sound(level, pos, ModSounds.RELAY_CLICK.get(), random, 0.04F, 0.15F, 1.4F);
            }
            case OUT_OF_BAND -> {
            }
            default -> {
            }
        }
    }

    /** A splash at the wellhead while it pumps; a dry click when it has no power. */
    private static void well(MachineStatus status, Level level, BlockPos pos, RandomSource random) {
        double x = pos.getX() + 0.5;
        double z = pos.getZ() + 0.5;
        switch (status) {
            case RUNNING -> {
                if (random.nextFloat() < 0.5F) {
                    particle(level, ParticleTypes.SPLASH, x, pos.getY() + 0.7, z,
                            (random.nextDouble() - 0.5) * 0.1, 0.08, (random.nextDouble() - 0.5) * 0.1);
                }
                sound(level, pos, ModSounds.MACHINE_HUM.get(), random, 0.04F, 0.2F, 0.7F);
            }
            case BLOCKED -> {
                if (random.nextFloat() < 0.15F) {
                    particle(level, ParticleTypes.SPLASH, x, pos.getY() + 0.8, z, 0.0, 0.02, 0.0);
                }
            }
            case STARVED -> sound(level, pos, ModSounds.RELAY_CLICK.get(), random, 0.03F, 0.12F, 0.5F);
            case OUT_OF_BAND -> {
            }
            default -> {
            }
        }
    }

    /** Heat above the coil while it holds a melt; a dry spark when power is missing. */
    private static void induction(MachineStatus status, Level level, BlockPos pos, RandomSource random) {
        double x = pos.getX() + 0.5;
        double z = pos.getZ() + 0.5;
        switch (status) {
            case RUNNING -> {
                if (random.nextFloat() < 0.45F) {
                    particle(level, ParticleTypes.FLAME, x, pos.getY() + 1.05, z, 0.0, 0.02, 0.0);
                }
                sound(level, pos, ModSounds.MACHINE_HUM.get(), random, 0.05F, 0.16F, 1.2F);
            }
            case BLOCKED -> {
                if (random.nextFloat() < 0.2F) {
                    particle(level, ParticleTypes.SMOKE, x, pos.getY() + 1.1, z, 0.0, 0.02, 0.0);
                }
            }
            case STARVED -> {
                if (random.nextFloat() < 0.2F) {
                    particle(level, ParticleTypes.ELECTRIC_SPARK, x, pos.getY() + 0.7, z, 0.0, 0.0, 0.0);
                }
                sound(level, pos, ModSounds.RELAY_CLICK.get(), random, 0.04F, 0.12F, 1.3F);
            }
            case OUT_OF_BAND -> {
            }
            default -> {
            }
        }
    }

    /** A drip into the mould while casting; a click when the mould or the melt is missing. */
    private static void caster(MachineStatus status, Level level, BlockPos pos, RandomSource random) {
        double x = pos.getX() + 0.5;
        double z = pos.getZ() + 0.5;
        switch (status) {
            case RUNNING -> {
                if (random.nextFloat() < 0.4F) {
                    particle(level, ParticleTypes.LAVA, x, pos.getY() + 0.7, z, 0.0, 0.0, 0.0);
                }
                sound(level, pos, ModSounds.MACHINE_HUM.get(), random, 0.04F, 0.14F, 0.9F);
            }
            case BLOCKED -> {
                if (random.nextFloat() < 0.15F) {
                    particle(level, ParticleTypes.SMOKE, x, pos.getY() + 0.8, z, 0.0, 0.01, 0.0);
                }
            }
            case STARVED -> sound(level, pos, ModSounds.RELAY_CLICK.get(), random, 0.03F, 0.12F, 0.7F);
            case OUT_OF_BAND -> {
            }
            default -> {
            }
        }
    }

    /** A soft draw of cloud while the cowl is running; haze when the oxygen buffer is full. */
    private static void intake(MachineStatus status, Level level, BlockPos pos, RandomSource random) {
        double x = pos.getX() + 0.5;
        double z = pos.getZ() + 0.5;
        switch (status) {
            case RUNNING -> {
                if (random.nextFloat() < 0.4F) {
                    particle(level, ParticleTypes.CLOUD, x, pos.getY() + 1.05, z, 0.0, 0.02, 0.0);
                }
                sound(level, pos, ModSounds.MACHINE_HUM.get(), random, 0.04F, 0.18F, 0.8F);
            }
            case BLOCKED -> {
                if (random.nextFloat() < 0.2F) {
                    particle(level, ParticleTypes.CLOUD, x, pos.getY() + 1.1, z, 0.0, 0.01, 0.0);
                }
            }
            case STARVED -> sound(level, pos, ModSounds.RELAY_CLICK.get(), random, 0.03F, 0.12F, 0.6F);
            case OUT_OF_BAND -> {
            }
            default -> {
            }
        }
    }

    private static void dynamo(MachineStatus status, Direction front, Level level, BlockPos pos,
                               RandomSource random) {
        double x = pos.getX() + 0.5 + front.getStepX() * 0.62;
        double z = pos.getZ() + 0.5 + front.getStepZ() * 0.62;
        if (status == MachineStatus.RUNNING) {
            if (random.nextFloat() < 0.5F) {
                particle(level, ParticleTypes.ELECTRIC_SPARK, x, pos.getY() + 0.55 + random.nextDouble() * 0.3, z,
                        (random.nextDouble() - 0.5) * 0.1, 0.02, (random.nextDouble() - 0.5) * 0.1);
            }
            sound(level, pos, ModSounds.RELAY_CLICK.get(), random, 0.12F, 0.25F, 0.9F);
        } else if (status == MachineStatus.BLOCKED) {
            if (random.nextFloat() < 0.3F) {
                particle(level, ParticleTypes.SMOKE, pos.getX() + 0.5, pos.getY() + 0.8, pos.getZ() + 0.5,
                        0.0, 0.03, 0.0);
            }
        }
    }

    private static void extractor(MachineStatus status, Level level, BlockPos pos, RandomSource random) {
        double x = pos.getX() + 0.82;
        double z = pos.getZ() + 0.82;
        switch (status) {
            case RUNNING -> {
                if (random.nextFloat() < 0.25F) {
                    particle(level, ParticleTypes.CAMPFIRE_COSY_SMOKE, x, pos.getY() + 1.05, z,
                            0.0, 0.06, 0.0);
                }
                sound(level, pos, ModSounds.MACHINE_HUM_HEAVY.get(), random, 0.08F, 0.35F, 0.8F);
            }
            case BLOCKED -> {
                if (random.nextFloat() < 0.4F) {
                    particle(level, ParticleTypes.LARGE_SMOKE, x, pos.getY() + 1.05, z, 0.0, 0.05, 0.0);
                }
            }
            case STARVED -> {
                if (random.nextFloat() < 0.15F) {
                    particle(level, ParticleTypes.SMOKE, x, pos.getY() + 1.05, z, 0.0, 0.02, 0.0);
                }
                sound(level, pos, ModSounds.RELAY_CLICK.get(), random, 0.04F, 0.2F, 0.6F);
            }
            case OUT_OF_BAND -> {
            }
            default -> {
            }
        }
    }

    private static void terminal(MachineStatus status, Direction front, Level level, BlockPos pos,
                                 RandomSource random) {
        double x = pos.getX() + 0.5 + front.getStepX() * 0.4;
        double z = pos.getZ() + 0.5 + front.getStepZ() * 0.4;
        switch (status) {
            case RUNNING -> {
                if (random.nextFloat() < 0.6F) {
                    // Glyphs drift in toward the screen, the way the enchanting table's do.
                    particle(level, ParticleTypes.ENCHANT, x, pos.getY() + 1.5, z,
                            -front.getStepX() * 0.3 + (random.nextDouble() - 0.5) * 0.3,
                            -0.6 + random.nextDouble() * 0.2,
                            -front.getStepZ() * 0.3 + (random.nextDouble() - 0.5) * 0.3);
                }
                sound(level, pos, ModSounds.MACHINE_HUM.get(), random, 0.05F, 0.2F, 1.4F);
            }
            case STARVED -> {
                if (random.nextFloat() < 0.2F) {
                    particle(level, ParticleTypes.ELECTRIC_SPARK, x, pos.getY() + 0.8, z, 0.0, 0.03, 0.0);
                }
                sound(level, pos, ModSounds.RELAY_CLICK.get(), random, 0.05F, 0.2F, 0.7F);
            }
            case BLOCKED -> {
                if (random.nextFloat() < 0.2F) {
                    particle(level, ParticleTypes.SMOKE, x, pos.getY() + 1.0, z, 0.0, 0.03, 0.0);
                }
            }
            case OUT_OF_BAND -> {
            }
            default -> {
            }
        }
    }

    private static void generator(MachineStatus status, Level level, BlockPos pos, RandomSource random) {
        double x = pos.getX() + 0.3;
        double z = pos.getZ() + 0.3;
        if (status == MachineStatus.RUNNING) {
            if (random.nextFloat() < 0.45F) {
                particle(level, ParticleTypes.FLAME, x + 0.4, pos.getY() + 1.05, z + 0.4,
                        0.0, 0.02, 0.0);
            }
            if (random.nextFloat() < 0.35F) {
                particle(level, ParticleTypes.CAMPFIRE_COSY_SMOKE, x + 0.4, pos.getY() + 1.15, z + 0.4,
                        0.0, 0.05, 0.0);
            }
            sound(level, pos, ModSounds.MACHINE_HUM.get(), random, 0.08F, 0.3F, 0.7F);
        } else if (status == MachineStatus.BLOCKED) {
            if (random.nextFloat() < 0.3F) {
                particle(level, ParticleTypes.LARGE_SMOKE, pos.getX() + 0.5, pos.getY() + 1.0,
                        pos.getZ() + 0.5, 0.0, 0.04, 0.0);
            }
        }
    }

    private static void mill(MachineStatus status, Level level, BlockPos pos, RandomSource random) {
        if (status == MachineStatus.RUNNING) {
            if (random.nextFloat() < 0.35F) {
                particle(level, ParticleTypes.CRIT, pos.getX() + 0.5, pos.getY() + 0.7, pos.getZ() + 0.5,
                        (random.nextDouble() - 0.5) * 0.15, 0.02, (random.nextDouble() - 0.5) * 0.15);
            }
            sound(level, pos, ModSounds.MACHINE_HUM_HEAVY.get(), random, 0.1F, 0.35F, 1.1F);
        } else if (status == MachineStatus.STARVED) {
            sound(level, pos, ModSounds.RELAY_CLICK.get(), random, 0.04F, 0.2F, 0.65F);
        } else if (status == MachineStatus.BLOCKED) {
            if (random.nextFloat() < 0.3F) {
                particle(level, ParticleTypes.SMOKE, pos.getX() + 0.5, pos.getY() + 0.9, pos.getZ() + 0.5,
                        0.0, 0.03, 0.0);
            }
        }
    }

    private static void furnace(MachineStatus status, Level level, BlockPos pos, RandomSource random) {
        if (status == MachineStatus.RUNNING) {
            if (random.nextFloat() < 0.5F) {
                particle(level, ParticleTypes.ELECTRIC_SPARK,
                        pos.getX() + 0.3 + random.nextDouble() * 0.4,
                        pos.getY() + 0.55 + random.nextDouble() * 0.3,
                        pos.getZ() + 0.3 + random.nextDouble() * 0.4,
                        0.0, 0.01, 0.0);
            }
            sound(level, pos, ModSounds.MACHINE_HUM_HEAVY.get(), random, 0.1F, 0.4F, 0.55F);
        } else if (status == MachineStatus.STARVED) {
            if (random.nextFloat() < 0.15F) {
                particle(level, ParticleTypes.SMOKE, pos.getX() + 0.5, pos.getY() + 0.8, pos.getZ() + 0.5,
                        0.0, 0.02, 0.0);
            }
            sound(level, pos, ModSounds.RELAY_CLICK.get(), random, 0.04F, 0.2F, 0.5F);
        } else if (status == MachineStatus.BLOCKED) {
            if (random.nextFloat() < 0.35F) {
                particle(level, ParticleTypes.LARGE_SMOKE, pos.getX() + 0.5, pos.getY() + 1.0,
                        pos.getZ() + 0.5, 0.0, 0.04, 0.0);
            }
        }
    }

    private static void belt(MachineStatus status, Level level, BlockPos pos, RandomSource random) {
        if (status == MachineStatus.RUNNING && random.nextFloat() < 0.15F) {
            particle(level, ParticleTypes.CRIT,
                    pos.getX() + 0.2 + random.nextDouble() * 0.6,
                    pos.getY() + 0.28,
                    pos.getZ() + 0.2 + random.nextDouble() * 0.6,
                    0.0, 0.01, 0.0);
        }
    }

    private static void manipulator(MachineStatus status, Direction front, Level level, BlockPos pos,
                                    RandomSource random) {
        if (status == MachineStatus.RUNNING && random.nextFloat() < 0.25F) {
            particle(level, ParticleTypes.CRIT,
                    pos.getX() + 0.5 + front.getStepX() * 0.35,
                    pos.getY() + 0.55,
                    pos.getZ() + 0.5 + front.getStepZ() * 0.35,
                    0.0, 0.02, 0.0);
        }
    }

    private static void pylon(int tier, MachineStatus status, Level level, BlockPos pos, RandomSource random) {
        double x = pos.getX() + 0.5;
        double z = pos.getZ() + 0.5;
        double top = pos.getY() + 0.75 + tier * 0.08;
        if (status == MachineStatus.RUNNING) {
            for (int i = 0; i < tier; i++) {
                if (random.nextFloat() < 0.5F) {
                    particle(level, ParticleTypes.END_ROD, x + (random.nextDouble() - 0.5) * 0.15, top,
                            z + (random.nextDouble() - 0.5) * 0.15, 0.0, 0.04 + 0.01 * tier, 0.0);
                }
            }
            // Higher tiers sit lower in pitch: a bigger pylon should sound bigger.
            sound(level, pos, ModSounds.PYLON_LINK.get(), random, 0.05F, 0.3F, 1.3F - 0.2F * tier);
        } else if (status == MachineStatus.STARVED) {
            for (int i = 0; i < tier; i++) {
                particle(level, ParticleTypes.ELECTRIC_SPARK, x + (random.nextDouble() - 0.5) * 0.5,
                        pos.getY() + 0.3 + random.nextDouble() * 0.6, z + (random.nextDouble() - 0.5) * 0.5,
                        0.0, 0.0, 0.0);
            }
            sound(level, pos, ModSounds.BROWNOUT_ALARM.get(), random, 0.03F, 0.4F, 1.0F);
        }
    }

    /**
     * Out-of-band is the same on every machine: purple motes and the brownout alarm at a higher
     * pitch. Conditions being wrong is not a machine-specific failure, so it does not get a
     * machine-specific effect.
     */
    private static void outOfBand(Level level, BlockPos pos, RandomSource random) {
        if (random.nextFloat() < 0.5F) {
            particle(level, ParticleTypes.WITCH,
                    pos.getX() + 0.2 + random.nextDouble() * 0.6,
                    pos.getY() + 0.4 + random.nextDouble() * 0.6,
                    pos.getZ() + 0.2 + random.nextDouble() * 0.6,
                    0.0, 0.04, 0.0);
        }
        sound(level, pos, ModSounds.BROWNOUT_ALARM.get(), random, 0.08F, 0.25F, 1.6F);
    }

    private static void particle(Level level, ParticleOptions type, double x, double y, double z,
                                 double dx, double dy, double dz) {
        level.addParticle(type, x, y, z, dx, dy, dz);
    }

    /** One-shot positional sound, played with some chance so a sound is a texture, not a metronome. */
    private static void sound(Level level, BlockPos pos, SoundEvent sound, RandomSource random,
                              float chance, float volume, float pitch) {
        if (random.nextFloat() < chance) {
            level.playLocalSound(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, sound,
                    SoundSource.BLOCKS, volume, pitch + (random.nextFloat() - 0.5F) * 0.1F, false);
        }
    }
}
