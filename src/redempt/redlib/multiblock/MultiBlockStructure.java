package redempt.redlib.multiblock;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.material.MaterialData;

import redempt.redlib.RedLib;

/**
 * A utility class to create interactive multi-block structures
 * @author Redempt
 *
 */
@SuppressWarnings("deprecation")
public class MultiBlockStructure {
	
	/**
	 * Use this to get the info to build a multi-block structure.
	 * Should be hard-coded.
	 * @param start One bounding corner of the region
	 * @param end The other bounding corner of the region
	 * @return A string representing all of the block data for the region
	 */
	public static String stringify(Location start, Location end) {
		if (!start.getWorld().equals(end.getWorld())) {
			throw new IllegalArgumentException("Locations must be in the same  world");
		}
		int minX = Math.min(start.getBlockX(), end.getBlockX());
		int minY = Math.min(start.getBlockY(), end.getBlockY());
		int minZ = Math.min(start.getBlockZ(), end.getBlockZ());
		
		int maxX = Math.max(start.getBlockX(), end.getBlockX());
		int maxY = Math.max(start.getBlockY(), end.getBlockY());
		int maxZ = Math.max(start.getBlockZ(), end.getBlockZ());
		
		int midVersion = Integer.parseInt(RedLib.getServerVersion().split("\\.")[1]);
		
		String output = (maxX - minX + 1) + "x" + (maxY - minY + 1) + "x" + (maxZ - minZ + 1) + ";";
		for (int x = minX; x <= maxX; x++) {
			for (int y = minY; y <= maxY; y++) {
				for (int z = minZ; z <= maxZ; z++) {
					Block block = new Location(start.getWorld(), x, y, z).getBlock();
					if (midVersion >= 13) {
						output += block.getBlockData().getAsString() + ";";
					} else {
						output += block.getType() + ":" + block.getData();
					}
				}
			}
		}
		output = minify(output);
		return output.substring(0, output.length() - 1);
	}
	
	/**
	 * Creates a MultiBlockStructure instance from an info string
	 * @param info The info string. Get this from {@link MultiBlockStructure#stringify(Location, Location)}
	 * @param name The name of the multi-block structure
	 * @return
	 */
	public static MultiBlockStructure create(String info, String name) {
		return new MultiBlockStructure(info, name, true, false);
	}
	
	/**
	 * Creates a MultiBlockStructure instance from an info string
	 * @param info The info string. Get this from {@link MultiBlockStructure#stringify(Location, Location)}
	 * @param name The name of the multi-block structure
	 * @param strictMode Whether block data is taken into account. Only checks material if false. Defaults to true.
	 * @return
	 */
	public static MultiBlockStructure create(String info, String name, boolean strictMode) {
		return new MultiBlockStructure(info, name, strictMode, false);
	}
	
	/**
	 * Creates a MultiBlockStructure instance from an info string
	 * @param info The info string. Get this from {@link MultiBlockStructure#stringify(Location, Location)}
	 * @param name The name of the multi-block structure
	 * @param strictMode Whether block data is taken into account. Only checks material if false. Defaults to true.
	 * @param ignoreAir If true, air in the original structure is skipped when checking blocks. Defaults to false.
	 * @return
	 */
	public static MultiBlockStructure create(String info, String name, boolean strictMode, boolean ignoreAir) {
		return new MultiBlockStructure(info, name, strictMode, ignoreAir);
	}
	
	private static String minify(String data) {
		String[] split = data.replace("minecraft:", "").split(";");
		int same = 0;
		String output = split[0] + ";";
		for (int i = 1; i < split.length - 1; i++) {
			if (split[i].equals(split[i + 1])) {
				same += same == 0 ? 2 : 1;
				continue;
			} else if (same > 0) {
				output += split[i - 1] + "*" + same + ";";
				same = 0;
				continue;
			}
			output += split[i] + ";";
		}
		if (same > 0) {
			output += split[split.length - 1] + "*" + same + ";";
		} else {
			output += split[split.length - 1];
		}
		Map<String, Integer> count = new HashMap<>();
		split = output.split(";");
		for (int i = 1; i < split.length; i++) {
			String str = split[i];
			if (str.contains("*")) {
				str = str.substring(0, str.indexOf('*'));
			}
			if (!count.containsKey(str)) {
				count.put(str, 1);
				continue;
			}
			count.put(str, count.get(str) + 1);
		}
		List<String> replace = new ArrayList<>();
		for (Entry<String, Integer> entry : count.entrySet()) {
			if (entry.getValue() >= 2) {
				replace.add(entry.getKey());
			}
		}
		String prepend = "";
		for (int i = 0; i < replace.size(); i++) {
			String str = replace.get(i);
			prepend += str + ";";
			output = output.replace(str, i + "");
		}
		if (replace.size() > 0) {
			output = "(" + prepend.substring(0, prepend.length() - 1) + ")" + output;
		}
		return output;
	}
	
	public static String expand(String data) {
		List<String> replace = null;
		if (data.startsWith("(")) {
			String list = data.substring(1, data.indexOf(')'));
			String[] split = list.split(";");
			replace = Arrays.asList(split);
			data = data.substring(data.indexOf(')') + 1);
		}
		String output = "";
		for (String str : data.split(";")) {
			String[] split = str.split("\\*");
			String val = "";
			try {
				int index = Integer.parseInt(split[0]);
				val = replace.get(index); 
			} catch (NumberFormatException e) {
				val = split[0];
			}
			if (split.length > 1) {
				int times = Integer.parseInt(split[1]);
				for (int i = 0; i < times; i++) {
					output += val + ";";
				}
				continue;
			}
			output += val + ";";
		}
		return output;
	}
	
	private String[][][] data;
	private String dataString;
	private String name;
	private int dimX;
	private int dimY;
	private int dimZ;
	private boolean strictMode = true;
	private boolean ignoreAir = false;
	
	private MultiBlockStructure(String info, String name, boolean strictMode, boolean ignoreAir) {
		info = expand(info);
		this.dataString = info;
		this.name = name;
		this.strictMode = strictMode;
		String[] split = info.split(";");
		String[] dimSplit = split[0].split("x");
		dimX = Integer.parseInt(dimSplit[0]);
		dimY = Integer.parseInt(dimSplit[1]);
		dimZ = Integer.parseInt(dimSplit[2]);
		data = parse(info);
	}
	
	private static String[][][] parse(String info) {
		String[] split = info.split(";");
		String[] dimSplit = split[0].split("x");
		int dimX = Integer.parseInt(dimSplit[0]);
		int dimY = Integer.parseInt(dimSplit[1]);
		int dimZ = Integer.parseInt(dimSplit[2]);
		
		String[][][] data = new String[dimX][dimY][dimZ];
		
		int pos = 1;
		for (int x = 0; x < dimX; x++) {
			for (int y = 0; y < dimY; y++) {
				for (int z = 0; z < dimZ; z++) {
					data[x][y][z] = split[pos];
					pos++;
				}
			}
		}
		return data;
	}
	
	/**
	 * Builds this multi-block structure at the given location
	 * @param loc The location to build the structure at
	 * @param rotation The number of 90-degree clockwise rotations to apply
	 * @param mirror Whether to mirror the structure on the X axis
	 */
	public void build(Location loc, int rotation, boolean mirror) {
		Rotator rotator = new Rotator(rotation, mirror);
		for (int x = 0; x < dimX; x++) {
			for (int y = 0; y < dimY; y++) {
				for (int z = 0; z < dimZ; z++) {
					rotator.setLocation(x, z);
					Location l = loc.clone().add(rotator.getRotatedX(), y, rotator.getRotatedZ());
					setBlock(l, data[x][y][z]);
				}
			}
		}
	}
	
	/**
	 * Builds this multi-block structure at the given location
	 * @param loc The location to build the structure at
	 */
	public void build(Location loc) {
		build(loc, 0, false);
	}
	
	/**
	 * Builds this multi-block structure at the given location
	 * @param loc The location to build the structure at
	 * @param rotation The number of 90-degree clockwise rotations to apply
	 */
	public void build(Location loc, int rotation) {
		build(loc, rotation, false);
	}
	
	/**
	 * Gets this multi-block structure's name. May be faster to compare this than to use .equals().
	 * @return The name of this multi-block structure
	 */
	public String getName() {
		return name;
	}
	
	/**
	 * Gets the dimensions of this multi-block structure. [x, y, z]
	 * @return The dimensions of this multi-block structure
	 */
	public int[] getDimensions() {
		return new int[] {dimX, dimY, dimZ};
	}
	
	/**
	 * Gets the Structure at the given block, if it exists.
	 * The given location can be any part of the multi-block structure.
	 * @param loc The location to check at
	 * @return The structure at this block, or null if it does not exist
	 */
	public Structure getAt(Location loc) {
		Block block = loc.getBlock();
		for (int rot = 0; rot < 4; rot++) {
			for (int x = 0; x < dimX; x++) {
				for (int y = 0; y < dimY; y++) {
					for (int z = 0; z < dimZ; z++) {
						Structure s;
						if (compare(data[x][y][z], block) && (s = test(loc, x, y, z, rot, false)) != null) {
							return s;
						}
					}
				}
			}
		}
		for (int rot = 0; rot < 4; rot++) {
			for (int x = 0; x < dimX; x++) {
				for (int y = 0; y < dimY; y++) {
					for (int z = 0; z < dimZ; z++) {
						Structure s;
						if (compare(data[x][y][z], block) && (s = test(loc, x, y, z, rot, true)) != null) {
							return s;
						}
					}
				}
			}
		}
		return null;
	}
	
	private Structure test(Location loc, int xPos, int yPos, int zPos, int rotation, boolean mirror) {
		Rotator rotator = new Rotator(rotation, mirror);
		for (int x = 0; x < dimX; x++) {
			for (int y = 0; y < dimY; y++) {
				for (int z = 0; z < dimZ; z++) {
					rotator.setLocation(x - xPos, z - zPos);
					int xp = rotator.getRotatedX();
					int yp = y - yPos;
					int zp = rotator.getRotatedZ();
					Block block = loc.clone().add(xp, yp, zp).getBlock();
					if (!compare(data[x][y][z], block)) {
						return null;
					}
				}
			}
		}
		rotator.setLocation(xPos, zPos);
		loc = loc.subtract(rotator.getRotatedX(), yPos, rotator.getRotatedZ());
		return new Structure(this, loc, rotator);
	}
	
	private boolean compare(String data, Block block) {
		int midVersion = Integer.parseInt(RedLib.getServerVersion().split("\\.")[1]);
		if (midVersion >= 13) {
			data = data.startsWith("minecraft:") ? data : ("minecraft:" + data);
			if (!strictMode) {
				return block.getType() == Bukkit.createBlockData(data).getMaterial();
			}
			if (ignoreAir && Bukkit.createBlockData(data).getMaterial() == Material.AIR) {
				return true;
			}
			return block.getBlockData().getAsString().equals(data);
		} else {
			String[] split = data.split(":");
			if (!strictMode) {
				return block.getType() == Material.valueOf(split[0]);
			}
			if (ignoreAir && split[0].equals("AIR")) {
				return true;
			}
			return block.getType() == Material.valueOf(split[0]) && block.getData() == Byte.parseByte(split[1]);
		}
	}
	
	@Override
	public boolean equals(Object o) {
		if (o instanceof MultiBlockStructure) {
			MultiBlockStructure structure = (MultiBlockStructure) o;
			return structure.dataString.equals(dataString);
		}
		return super.equals(o);
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(dataString, name);
	}
	
	@Override
	public String toString() {
		return dataString;
	}
	
	private void setBlock(Location loc, String data) {
		int midVersion = Integer.parseInt(RedLib.getServerVersion().split("\\.")[1]);
		if (midVersion >= 13) {
			loc.getBlock().setBlockData(Bukkit.createBlockData(data));
		} else {
			String[] split = data.split(":");
			Material type = Material.valueOf(split[0]);
			byte dataValue = Byte.parseByte(split[1]);
			BlockState state = loc.getBlock().getState();
			state.setData(new MaterialData(type, dataValue));
			state.update();
		}
	}
	
	public static class Rotator {
		
		private static String[] rotations = {"x,z", "-z,x", "-x,-z", "z,-x"};
		
		private int rotation;
		private boolean mirrored;
		private int x = 0;
		private int z = 0;
		
		protected Rotator(int rotation, boolean mirrored) {
			while (rotation < 0) {
				rotation += 4;
			}
			this.rotation = rotation % 4;
			this.mirrored = mirrored;
		}
		
		/**
		 * Sets the relative coordinates this Rotator will rotate
		 * @param x The relative X coordinate
		 * @param z The relative Z coordinate
		 */
		public void setLocation(int x, int z) {
			this.x = mirrored ? -x : x;
			this.z = z;
		}
		
		/**
		 * Gets the rotated relative X
		 * @return The rotated relative X
		 */
		public int getRotatedX() {
			String rotationString = rotations[rotation];
			String xString = rotationString.split(",")[0];
			int val = 0;
			switch (xString.charAt(xString.length() - 1)) {
				case 'x':
					val = x;
					break;
				case 'z':
					val = z;
					break;
			}
			return xString.startsWith("-") ? -val : val;
		}
		
		/**
		 * Gets the rotated relative Z
		 * @return The rotated relative Z
		 */
		public int getRotatedZ() {
			String rotationString = rotations[rotation];
			String xString = rotationString.split(",")[1];
			int val = 0;
			switch (xString.charAt(xString.length() - 1)) {
				case 'x':
					val = x;
					break;
				case 'z':
					val = z;
					break;
			}
			return xString.startsWith("-") ? -val : val;
		}
		
		/**
		 * Gets a Rotator which will negate the operations of this Rotator
		 * @return The inverse Rotator
		 */
		public Rotator getInverse() {
			return new Rotator(-rotation, mirrored);
		}
		
		/**
		 * Gets a clone of this Rotator
		 * @return The clone of this Rotator
		 */
		public Rotator clone() {
			return new Rotator(rotation, mirrored);
		}
		
		/**
		 * Gets the rotation, in number of 90-degree clockwise rotations
		 * @return The rotation
		 */
		public int getRotation() {
			return rotation;
		}
		
		/**
		 * Gets whether this rotator mirrors over the X axis
		 * @return Whether this rotator mirrors over the X axis
		 */
		public boolean isMirrored() {
			return mirrored;
		}
		
	}
	
}