package net.enelson.sopcrates.utils;

import java.util.Arrays;
import java.util.List;

public enum CheckType {

	HAS_PERM(Arrays.asList("has perm", "has permission", "hasperm", "haspermission")),
	HAS_NO_PERM(Arrays.asList("!has perm", "!has permission", "!hasperm", "!haspermission")),
	STRING_EQUALS(Arrays.asList("string equals", "stringequals")),
	STRING_NOT_EQUALS(Arrays.asList("!string equals", "!stringequals"));

	private List<String> identifier;

	CheckType(List<String> identifier) {
		this.identifier = identifier;
	}
	
	private List<String> getIdentifiers() {
		return this.identifier;
	}

	public static CheckType getType(String s) {
		for (CheckType type : values()) {
			for (String id : type.getIdentifiers()) {
				if (s.equalsIgnoreCase(id)) {
					return type;
				}
			}
		}
		return null;
	}
}
