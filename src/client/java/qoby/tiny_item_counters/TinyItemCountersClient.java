package qoby.tiny_item_counters;

import net.fabricmc.api.ClientModInitializer;

public class TinyItemCountersClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		TinyItemCountersConfig.load();
	}
}