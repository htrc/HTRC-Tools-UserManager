package org.hathitrust.htrc.tools.usermanager.utils;

public enum ResourceActionPermission {

	GET 			(2),
	PUT 			(3),
	DELETE 			(4),
	AUTHORIZE 		(5);

	private final int _id;

	private ResourceActionPermission(int id) {
		_id = id;
	}

	public String getPermission() {
		return Integer.toString(_id);
	}

	@Override
	public String toString() {
		return getPermission();
	}
}
