BufferRegistry {
	classvar <all;
	var name, <server, defaultChangedAction;
	var dict, <constructorDict;
	
	*initClass {
		Class.initClassTree(Dictionary);
		all = IdentityDictionary();
	}
	
	*get {
		| key=\default, server=\default |
		var br;
		
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
		| inServer=\default, inName |
		var defaultChangedFunc;
		dict = Dictionary();
		constructorDict = Dictionary();

		defaultChangedAction = {
			| oldServer, message, newServer |
			if (message == \default, {
				"default changed from % to %\n".postf(oldServer, newServer);
				this.server_(\default);
			});				
		};

		name = inName;		
		this.server = inServer;	
	}
	
	at {
		| key |
		^dict.at(key);
	}
	
	read {
		| name, path, startFrame=0, numFrames=(-1) |
		constructorDict[name] = [\read, path, startFrame, numFrames];
		server.makeBundle(nil, {
			if (dict[name].notNil, {
				dict[name].allocRead(path, startFrame, numFrames);
				dict[name].updateInfo();
			}, {
				dict[name] = Buffer.read(server, path, startFrame, numFrames);
			});
		});
		^dict[name];
	}
	
	alloc {
		| name, numFrames, numChannels=1 |
		constructorDict[name] = [\alloc, numFrames, numChannels=1];
		server.makeBundle(nil, {
			if (dict[name].notNil, {
				dict[name].alloc(numFrames, numChannels);
			}, {
				dict[name] = Buffer.alloc(server, numFrames, numChannels);
			});
		});
		^dict[name];
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
	
	nums {
		^dict.values.collect(_.bufnum);
	}
	
	doOnServerBoot {
		server.makeBundle(nil, {
			this.constructorDict.keys().do({
				| key |
				var constructor;
				dict[key].free;
				dict[key] = nil;
				constructor = constructorDict[key];
				this.perform(constructor[0], key, *constructor[1..]);
			});
		});
	}
	
	doOnServerQuit {
	}
	
	server_{
		| newServer, followDefault=false |
		if (newServer != server, {
			if (newServer == \default, {
				this.server_(Server.default, true);
				^this;
			});
			
			if (server.notNil, {
				ServerBoot.remove(this, server);
				ServerQuit.remove(this, server);
				
				server.removeDependant(defaultChangedAction);
			});

			if (followDefault, {
				newServer.addDependant(defaultChangedAction);
			});

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