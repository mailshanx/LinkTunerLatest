package org.arl.modem.linktuner;

public interface Notifiable<E> {
	public void handleLinktunerNotifications(E ntf);
}
