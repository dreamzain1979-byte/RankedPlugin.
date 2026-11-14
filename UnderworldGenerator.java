package dev.rankedsmp;

import org.bukkit.Material;
import org.bukkit.block.Biome;
import org.bukkit.generator.BiomeProvider;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.generator.WorldInfo;
import org.bukkit.util.noise.SimplexOctaveGenerator;

import java.util.List;
import java.util.Random;

/**
 * Dark cavern world: deepslate / obsidian floor with sculk and lava pockets, an obsidian-capped ceiling
 * and a bedrock lid. Uses the Nether dimension type (no skylight, eerie fixed lighting) and the Soul Sand
 * Valley biome so the fog is a dark teal.
 */
public final class UnderworldGenerator extends ChunkGenerator {
    private volatile SimplexOctaveGenerator floorNoise;
    private volatile SimplexOctaveGenerator ceilNoise;
    private volatile SimplexOctaveGenerator surfNoise;

    private synchronized void init(long seed) {
        if (floorNoise != null) return;
        SimplexOctaveGenerator f = new SimplexOctaveGenerator(seed, 4);
        f.setScale(1.0 / 96.0);
        SimplexOctaveGenerator c = new SimplexOctaveGenerator(seed + 1, 3);
        c.setScale(1.0 / 128.0);
        SimplexOctaveGenerator s = new SimplexOctaveGenerator(seed + 2, 2);
        s.setScale(1.0 / 24.0);
        ceilNoise = c;
        surfNoise = s;
        floorNoise = f;
    }

    @Override
    public void generateNoise(WorldInfo info, Random random, int chunkX, int chunkZ, ChunkData data) {
        if (floorNoise == null) init(info.getSeed());
        int minY = data.getMinHeight();
        int maxY = data.getMaxHeight(); // exclusive
        int top = maxY - 1;
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int wx = chunkX * 16 + x;
                int wz = chunkZ * 16 + z;
                double f = floorNoise.noise(wx, wz, 0.5, 0.5, true);
                double c = ceilNoise.noise(wx, wz, 0.5, 0.5, true);
                double s = surfNoise.noise(wx, wz, 0.5, 0.5, true);
                int floorH = Math.max(minY + 8, Math.min(top - 60, 48 + (int) (f * 16)));
                int ceilH = Math.min(top - 4, Math.max(floorH + 60, 205 + (int) (c * 14)));
                int hash = (wx * 734287 + wz * 912391) ^ (wx * wz * 31);

                data.setBlock(x, minY, z, Material.BEDROCK);
                for (int y = minY + 1; y <= floorH; y++) {
                    Material m = Material.DEEPSLATE;
                    if (y > floorH - 2) {
                        if (s > 0.30) m = Material.OBSIDIAN;
                        else if (s < -0.55) m = Material.SCULK;
                        else if (s > 0.18 && (hash & 15) == 0) m = Material.CRYING_OBSIDIAN;
                    } else if ((hash + y * 17) % 53 == 0) {
                        m = Material.OBSIDIAN;
                    }
                    data.setBlock(x, y, z, m);
                }
                if (f < -0.55) {
                    for (int y = floorH + 1; y <= 44; y++) data.setBlock(x, y, z, Material.LAVA);
                }
                for (int y = ceilH; y < top; y++) {
                    data.setBlock(x, y, z, y <= ceilH + 1 ? Material.OBSIDIAN : Material.DEEPSLATE);
                }
                data.setBlock(x, top, z, Material.BEDROCK);
            }
        }
    }

    @Override
    public BiomeProvider getDefaultBiomeProvider(WorldInfo worldInfo) {
        return new BiomeProvider() {
            @Override
            public Biome getBiome(WorldInfo info, int x, int y, int z) {
                return Biome.SOUL_SAND_VALLEY;
            }

            @Override
            public List<Biome> getBiomes(WorldInfo info) {
                return List.of(Biome.SOUL_SAND_VALLEY);
            }
        };
    }
}
