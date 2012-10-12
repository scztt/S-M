BufferRegistry {
	classvar <all;
	var name, <server;
	var dict, <constructorDict;
	
	*initClass {
		Class.initClassTree(Dictionary);
		all = IdentityDictionary();
	}
	
	*get {
		| key, server |
		var br;
		key = key ? \default;
		server = server ? Server.default;
		
		if (all[key].isNil, {
			all[key] = super.new.init(server, key);
		});
		br = all[key];
		
		if (server != br.server, {
			br.server_(server);
		});
		
		^br;
	}
	
	*new {
		| ...args |
		^this.get(*args);
	}
	
	init {
		| inServer, inName |
		server = inServer ? Server.default;
		name = inName;
		
		dict = Dictionary();
		constructorDict = Dictionary();
		
		ServerBoot.add(this, server);
		ServerQuit.add(this, server);	
	}
	
	at {
		| key |
		^dict.at(key);
	}
	
	read {
		| key, path, startFrame=0, numFrames=(-1) |
		constructorDict[key] = [\read, path, startFrame, numFrames];
		server.makeBundle(nil, {
			if (dict[key].notNil, {
				dict[key].allocRead(path, startFrame, numFrames);
				dict[key].updateInfo();
			}, {
				dict[key] = Buffer.read(server, path, startFrame, numFrames);
			});
		});
		^dict[key];
	}
	
	alloc {
		| key, numFrames, numChannels=1 |
		constructorDict[key] = [\alloc, numFrames, numChannels=1];
		server.makeBundle(nil, {
			if (dict[key].notNil, {
				dict[key].alloc(numFrames, numChannels);
			}, {
				dict[key] = Buffer.alloc(server, numFrames, numChannels);
			});
		});
		^dict[key];
	}
	
	remove {
		| key |
		var removed;
		if (dict[key].notNil, {
			removed = dict[key];
			dict.removeAt(key);
			constructorDict.removeAt(key);
			
			if (server.serverRunning, {
				removed.free;
			});
			
			^removed;
		});
		^nil;
	}
	
	keys {
		^dict.keys();
	}
	
	values {
		^dict.values();
	}
	
	doOnServerBoot {
		server.makeBundle(nil, {
			this.keys().do({
				| key |
				var constructor;
				dict[key] = nil;
				constructor = constructorDict[key];
				this.perform(constructor[0], key, *constructor[1..]);
			});
		});
	}
	
	doOnServerQuit {
	}
	
	clear {
		server.makeBundle(nil, {
			this.keys.do({
				| key |
				this.remove(key);
			})
		});
	}
	
	server_{
		| newServer |
		if (newServer != server, {
			ServerBoot.remove(this, server);
			ServerQuit.remove(this, server);

			ServerBoot.add(this, newServer);
			ServerQuit.add(this, newServer);
			
			server = newServer;
			
			if (server.serverRunning, {
				this.doOnServerBoot();
			});
			"Server of registry % changed to %.\n".postf(name, server);
		})
	}
}