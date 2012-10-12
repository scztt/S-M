BufferRegistry {
	var dictionary;
	var registerAction, unregisterAction;
	
	
	*new {
		| server ...args |		
		^super.new(args).init;
	}
	
	init {
		dictionary = IdentityDictionary();
	}
	
	at {
		| key |
		^dictionary.at(key);
	}
	
	read {
		| key, path, startFrame, numFrames=(-1) |
		if (dictionary[key].notNil, {
			dictionary[key].allocRead(path, startFrame, numFrames);
		}, {
//			dictionary[key] = Buffer.read(li
		});
	}
	
	remove {
		| key |
	}
	
	keys {
	}
}