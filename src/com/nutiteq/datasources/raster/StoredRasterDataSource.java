package com.nutiteq.datasources.raster;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;

import android.os.Environment;

import com.nutiteq.components.MapPos;
import com.nutiteq.components.MapTile;
import com.nutiteq.components.TileBitmap;
import com.nutiteq.log.Log;
import com.nutiteq.projections.Projection;
import com.nutiteq.rasterdatasources.AbstractRasterDataSource;
import com.nutiteq.utils.MGMUtils;
import com.nutiteq.utils.Utils;

/**
 * A raster data source class that uses the SDCard a source for the map tile data. The map data has to be in MGMaps format.
 * The request are generated in this manner:
 * <p>
 * <p>
 * path + name + "_" + zoom + "/" + tpfx + "_" + tpxy + ".mgm"
 * <p>
 * <p>
 * For example if: path = "/london/", name = "OpenStreetMap"
 * <p>
 * "/london/" + "OpenStreetMap" + "_" + "0" + "/" + "0" + "_" + "0" + ".mgm"
 * <p>
 * Result: /london/OpenStreetMap_0/0_0.mgm
 */
public class StoredRasterDataSource extends AbstractRasterDataSource {

    private static final String CONFIG_FILENAME = "cache.conf";
    private static final String FILE_EXT = ".mgm";
    private static final int BUFFER_SIZE = 4096;

    private int tileSize;
    private int tilesPerFile = 1;
    private int hashSize = 1;

    // tiles per file X & Y
    private int tpfx = 1;
    private int tpfy = 1;

    private String location;
    private String name;
    private MapPos center;

    /**
     * Class constructor. Creates a new data source that uses a specified folder on the SDCard as a source for the tile
     * data. The tile size is used to determine the naming convention for the tile files. Tiles that are out of the
     * specified minimum / maximum zoom range are not downloaded. The id used should be unique to each rasterlayer, if two
     * or more raster layers use the same id, they will also share the tiles in the cache. The map data must be in MGMaps
     * format.
     * 
     * @param projection
     *          the desired projection
     * @param tileSize
     *          the tile size in pixels
     * @param minZoom
     *          the minimum zoom
     * @param maxZoom
     *          the maximum zoom
     * @param name
     *          the name of the sub folders
     * @param path
     *          the path to the main map folder
     */
    public StoredRasterDataSource(Projection projection, int tileSize, int minZoom, int maxZoom, String name, String path) {
        super(projection, minZoom, maxZoom);
        this.location = path;
        this.tileSize = tileSize;
        this.name = name;
        readConfig();
    }
    
    public MapPos getCenter() {
        return center;
    }

    @Override
    public TileBitmap loadTile(MapTile tile) {
        final StringBuffer result = new StringBuffer(location);
        result.append(name);
        result.append('_');
        result.append(tile.zoom);
        result.append('/');
        if (hashSize > 1) {
            result.append((int) (tile.x * tileSize + tile.y) % hashSize);
            result.append('/');
        }
        result.append((tilesPerFile > 1) ? (tile.x / tpfx) : tile.x);
        result.append('_');
        result.append((tilesPerFile > 1) ? (tile.y / tpfy) : tile.y);
        result.append(FILE_EXT);
        String path = result.toString();
        Log.info("StoredMapLayer: Start loading" + path);
        int dx = tile.x % tpfx;
        int dy = tile.y % tpfy;

        String storageState = Environment.getExternalStorageState();
        if (!storageState.equals(Environment.MEDIA_MOUNTED) && !(storageState.equals(Environment.MEDIA_MOUNTED_READ_ONLY))) {
            Log.warning(getClass().getName() + ": Failed to fetch tile. " + "(SD Card not available)");
            return null;
        }

        File file = new File(path);
        try {
            FileInputStream inputStream = new FileInputStream(file);
            return readTileFromStream(inputStream, dx, dy);
        } catch (Exception e) {
            Log.error(getClass().getName() + ": Failed to read file " + path + " ex:" + e.getMessage());
        }

        return null;
    }

    private TileBitmap readTileFromStream(InputStream inputStream, int dx, int dy) {
        try {

            // Read header
            int toRead = 6 * tilesPerFile + 2;
            final byte[] header = new byte[toRead];
            long ch = 0;
            int rd = 0;
            while ((rd < toRead) && (ch >= 0)) {
                ch = inputStream.read(header, rd, toRead - rd);
                if (ch > 0) {
                    rd += ch;
                }
            }

            // Search for the tile
            final int numberOfTilesStored = (Utils.unsigned(header[0]) << 8) + Utils.unsigned(header[1]);
            int offset = -1;
            int offset2 = -1;
            final int n6 = numberOfTilesStored * 6;
            for (int i6 = 0; i6 < n6; i6 += 6) {
                if ((header[2 + i6] == dx || header[2 + i6] + 256 == dx)
                        && (header[3 + i6] == dy || header[3 + i6] + 256 == dy)) {
                    offset2 = (Utils.unsigned(header[4 + i6]) << 24) + (Utils.unsigned(header[5 + i6]) << 16)
                            + (Utils.unsigned(header[6 + i6]) << 8) + (Utils.unsigned(header[7 + i6]));
                    offset = (i6 == 0) ? toRead : ((Utils.unsigned(header[i6 - 2]) << 24)
                            + (Utils.unsigned(header[i6 - 1]) << 16) + (Utils.unsigned(header[i6]) << 8) + (Utils
                                    .unsigned(header[i6 + 1])));
                    break;
                }
            }

            if (offset < 0) {
                throw new IllegalArgumentException("Tile not found");
            }

            // Seek
            MGMUtils.skip(inputStream, offset - toRead, BUFFER_SIZE);

            // read data
            ch = 0;
            rd = 0;
            toRead = offset2 - offset;
            final byte[] result = new byte[toRead];
            while ((rd < toRead) && (ch >= 0)) {
                ch = inputStream.read(result, rd, (toRead - rd) > BUFFER_SIZE ? BUFFER_SIZE : (toRead - rd));
                if (ch > 0) {
                    rd += ch;
                }
            }
            return new TileBitmap(result);

        } catch (IOException e) {
            Log.error(getClass().getName() + ": Failed to fetch tile. " + e.getMessage());
        } finally {
            try {
                if (inputStream != null) {
                    inputStream.close();
                }
            } catch (IOException e) {
                Log.error(getClass().getName() + ": Failed to close the stream. " + e.getMessage());
            }
        }
        return null;
    }

    private void readConfig() {
        try {
            String storageState = Environment.getExternalStorageState();
            if (!storageState.equals(Environment.MEDIA_MOUNTED)
                    && !(storageState.equals(Environment.MEDIA_MOUNTED_READ_ONLY))) {
                Log.error(getClass().getName() + ": Failed to read config. " + "(SD Card not available)");
                return;
            }

            final String filename = location + CONFIG_FILENAME;

            FileInputStream fis = new FileInputStream("/" + filename);
            final byte[] data = MGMUtils.readFully(fis);
            fis.close();

            final String sdata = new String(data);
            final String[] lines = Utils.split(sdata, "\n");
            for (int i = 0; i < lines.length; i++) {
                // split into at most 2 tokens
                final String[] tokens = MGMUtils.split(lines[i].trim(), '=', false, 2);
                if (tokens.length == 2) {
                    final String name = tokens[0].trim().toLowerCase(Locale.US);
                    final String value = tokens[1].trim();

                    // ignore empty values
                    if (value.length() == 0) {
                        continue;
                    }

                    // ignore comments
                    if (name.startsWith("#")) {
                        continue;
                    }

                    if (name.equals("tiles_per_file")) {
                        final int tpf = Integer.parseInt(value);
                        if (tpf > 0 && (tpf & (-tpf)) == tpf) {
                            setTilesPerFile(tpf);
                        } else {
                            throw new IOException("Invalid tiles_per_file");
                        }
                    } else if (name.equals("hash_size")) {
                        final int hs = Integer.parseInt(value);
                        if (hs >= 1 && hs < 100) {
                            hashSize = hs;
                        } else {
                            throw new IOException("Invalid hash_size");
                        }
                    } else if (name.equals("center")) {
                        try {
                            final String[] xyz = MGMUtils.split(value.trim(), ',', false, 4);
                            double lat = Float.parseFloat(xyz[0].trim());
                            double lon = Float.parseFloat(xyz[1].trim());
                            int zoom = Integer.parseInt(xyz[2].trim());
                            Log.debug("center zoom found = " + lat + " " + lon + " " + zoom);
                            this.center = new MapPos(lat,lon,zoom);
                        } catch (final Exception ex) {

                            throw new IOException("Invalid center location");
                        }
                    }
                }
            }
        } catch (final IOException ex) {
            Log.error("StoredMap: Error in " + CONFIG_FILENAME + " " + ex.getMessage());
        }
    }

    private void setTilesPerFile(final int tilesPerFile) {
        this.tilesPerFile = tilesPerFile;
        final int tpflog = Utils.log2(tilesPerFile);
        tpfx = 1 << (tpflog / 2 + tpflog % 2);
        tpfy = 1 << (tpflog / 2);
    }

}
