#EchoX3 *(Summary)*
*.. is pronounced "Echo times three", or "Echo three" for short. In a pinch, you can even call it Echo if the context is clear.*

**EchoX3** is a distributed object cache that allows methods to be added to those objects so they can be called remotely. It is grounded on distributed hash-map like scalability, Java nio extreme performance and a simple flexible set of bulk (multiple keys) API. **EchoX3** allows for incredibly powerful applications across terabytes of data, running on hundreds of servers, with on-the-fly changes to the algorithms (code). All this supported by a fanatical dedication to zero planned/unplanned downtime and ease of operation for all parties involved in the lifecycle of your **EchoX3** application. It enables..
* Developers whose life is made simpler with simple APIs and built-in functionality
* The operations team who receives a high-reliability, easy to manage system
* The management team who has to make the easy decision to choose Trellis

##Guiding design principles
Guiding principles are guidelines to be used when making decision; they are tie breakers; they make the difference between a bug being marked “Won’t fix”, “Postponed” and “Must fix”.

* If you are counting milliseconds, you are already too slow!
* Don’t compromise on quality: We ship reliability!
* Ease of use for the user: Write the component in the units of the user of that component
	* The user of an API is a dev
	* The user of a monitoring console can be ops, application engineer, test  or dev
	* The user of configuration is application engineer
* Servers are compatible with clients one major version backwards e.g. a Server v3.2 is compatible with a client v 2.0
* Zero planned down-time; Zero un-planned down-time.
* Backend upgrades are fully compatible (following the upgrade instructions), with no loss of data and no downtime (see 4), at least with the previous release.
* All configuration settings (including all routing changes) are hot

#EchoX3 *(Overview)*
##What does **EchoX3** do?
**EchoX3** is a distributed object cache. The cache contains real objects (not byte[]). As the client makes calls to write new data to the object, it updates itself. During a read call, the object may return stored values or perform calculations and return the results of the calculations. The key is that a true object resides in the cache that can perform operations in-place.
When the client performs a call (see Figure 1), the **EchoX3** system uses the cache name and the key to find the object. The client’s request is then passed to the object (Figure 2).
![Figure 1 - System overview: Routing](https://cloud.githubusercontent.com/assets/7895210/8338052/4bf4f3a8-1a63-11e5-9437-1f857309b363.jpg)
####Figure 1 - System overview: Routing
![Figure 2 - System overview: Client request to cache object](https://cloud.githubusercontent.com/assets/7895210/8338053/4bf509f6-1a63-11e5-8a0c-1250469902e6.jpg)
####Figure 2 - System overview: Client request to cache object
To simplify the development effort, a number of logistics tasks are handled automatically by the **EchoX3** system:

* Connection management
* Client side retry
* Routing
* Object creation on the server
* Class definition management (no need to put your jar on the server)
* Synchronization (custom ReadWriteLock)
* Maintenance (e.g. Kick your object to clean-up itself)
* In-place object update - (i.e. Upgrade a user object from MyCacheObjectV1 to MyCacheObjectV2 while taking traffic)
* Local & Remote mode with identical semantics to facilitate development & debugging, and the occasional need for a production local cache.

In addition to the powerful object cache, **EchoX3** supports a simple cache mode, with the standard put/get functionality of a traditional cache.
Of course, each of the **EchoX3** APIs is made more powerful with a complement of efficient multi-key version of the API (aka bulk).

##Hash map tutorial
This section is for those who do not necessary deal with hash maps every day and may need a refresher on how they work. Those intimately familiar with hash map technology may skip this section.

	NOTE:	This section discusses a basic HashMap, not necessarily the HashMap or
			hash function used by Trellis or by the standard Java classes.

Much of the work done by Trellis is mapping from a key to some object. Obviously, from the user’s key to the user’s ITrellisCacheObject, and also at several places for management of internal structures. How does one search for the key? Some of the known techniques include

* Enumeration: Simply walking a complete list of the possible elements until the correct one is found. Statistically, one would have to walk about half the list to find the correct element. It is known as a O(n) solution. For example, if there are 1 million (1,000,000) items in a list, you will have to walk about 500,000 on average to find the correct one.
* Binary search: This can be accomplished by storing the elements in a binary tree or by storing them in a sorted array. Either way, on each test, one looks at the middle of the possibility and eliminates about half the range. If there are 2n elements, this will take n tests to find the correct element and is referred as a O(log(n)) solution. Getting back to our list of 1,000,000 items, the correct element can now be found in about 20 tries (220 = 1,048,576).

A hash map style algorithm is faster than the binary search algorithm, at the cost of requiring additional memory to manage the hash table.
First, one calculates the hash of the key. What is a hash? A hash is the result of a mathematical algorithm applied to the parts of the key. For example, if the key is a byte array and we choose the default Java hash function, the algorithm is..

	public static int hashCode(byte a[])
		{
			if (a == null)
				return 0;

			int result = 1;
			for (byte element : a)
				result = 31 * result + element;

			return result;
		}

This modulo of this number is used to distribute the keys amongst a set number of buckets. Given N buckets, one takes ABS(hash) % N to obtain the bucket number. For example, if the hash is -32 and there are 5 buckets, then the bucket number becomes ABS(-32) % 5 = 2.
In the simple case, each bucket contains a list of the items that map to the buckets. The power of the hash map is achieved when the number of buckets is sufficiently large that there are only very few items per buckets (0, 1 or 2). For example, the Java hash map defaults to a maximum occupancy of 0.75 items per bucket. When more items are placed in the map, Java automatically increases the number of buckets.
When searching within a hash map, one first uses the hash of the key to find the correct bucket, and then walks the list of items in the bucket for the one matching exactly the key. In a properly balanced hash map, this results in, statistically, a single compare to find the correct item. The hash map is said to be of order O(1). The time required to find an item is NOT dependent on the number of items in the map. Of course, more items result in larger the map and larger the memory requirement.

##Hello "EchoX3"
Every good programming manual starts with a HelloWorld program. This is where you make sure you include the proper libraries, perform the correct initialization and can make the most basic call into the new WhateverNewThingYouAreUsing.

	NOTE:	Listings for all samples discussed are in section 6. The code for the Hello applications
			can also be found under the module “Hello” as part of the EchoX3 distribution.

This section will walk you through "Hello Trellis", from your first program using local mode through setting-up a server and making remote calls to the remote server.

##Dependencies…
**EchoX3** is composed of two separate and distinct deliverables:
* EchoX3Client.jar

The client library used in your application
* EchoX3Server.war

The server application, which runs in a standard Tomcat container.
The self-imposed requirement on the client library (EchoX3Client.jar) is to have a minimal set of dependencies, such that the application using **EchoX3** has the freedom to run under whatever platform it wants. As such, the only dependencies at this point is
* log4j

The server on the other hand runs independently of user code and does not have the same limitation. Server dependencies will be determined later…
* TBD

##HelloSimple.java
Within EchoX3, the first step is always to obtain the factory object.

	IClientFactory	factory	= ClientFactory.getInstance();

When operating in local mode, it is necessary to tell Trellis how to configure the cache(s) that will be used. It is recommended that the call to putLocalConfiuration() be made in all cases, as it facilitate the switching between local and remote mode.

	// Set the configuration file for this cache, required only in local.
	URL			url		= ClassLoader.getSystemResource(FILE_NAME);
	factory.putLocalConfiguration(CACHE_NAME, url);

Next, the admin client is used to connect to the cache. This is required in either Local or Remote mode. 

	// Create the local cache; or connect to the remote cache; always required
	IAdminClient		admin		= factory.getAdminClient(ClientType.Local);
	admin.connectToCache(CACHE_NAME);

The simple cache client can now be used for put and get operations.

	ISimpleCacheClient	client		=factory.getSimpleClient(ClientType.Local);
	client.put(CACHE_NAME, KEY, VALUE);
	Serializable		back		= client.get(CACHE_NAME, KEY);

##HelloObject.java
The first few steps required in object mode are identical to simple mode. The only difference is the name of the file containing the configuration.

**EchoX3** comes with a built-in TestObject and matching TestObjectFactory. Each request to the TestObject include a burn time (micro-seconds spent burning CPU), sleep time (milli-seconds spent sleeping), a throw flag (telling the object if it should throw an exception). On a write, a byte array is sent to the object and the byte array is returned during a read operation. This object is intended for test programs, including this HelloObject.

The writeOnly call takes a writeRequest.

How do object get put in the object cache?

Objects are actually never put in an object cache by the user’s code. The client API only supports writeOnly() and readOnly(); not put. How do objects get created? Blank objects are created using the factory class specified in the configuration file when a key corresponds to an object that does not exists in the cache. Upon a write operation, rather than returning “not found”, the object cache will create a blank object and pass the write operation to the newly created object.

	IObjectCacheClient	client		= factory.getObjectClient(ClientType.Local);
	byte[]			bytes		= VALUE.getBytes(Charset.defaultCharset());
	TestWriteRequest	writeRequest	= new TestWriteRequest(0, 0, false, bytes);
	client.writeOnly(CACHE_NAME, KEY, writeRequest);

And the read request takes a readRequest to return a readResponse.

	TestReadRequest	readRequest	= new TestReadRequest(0, 0, false);
	Serializable		response	= client.readOnly(CACHE_NAME, KEY, readRequest);
	TestReadResponse	readResponse	= (TestReadResponse) response;
	byte[]			backBytes	= readResponse.getBytes();
	String			back		= new String(backBytes,Charset.defaultCharset());

##Hello EchoX3 server

	TODO

##Writing a EchoX3 application

The figure below illustrates the various components of a typical trellis application. In this section, we will build a complete EchoX3 application named SmartCache (included in the EchoX3 distribution).
![Figure 3 - Trellis application components](https://cloud.githubusercontent.com/assets/7895210/8338054/4bf62cdc-1a63-11e5-8b8f-1e6b7fbfbe7e.jpg)
####Figure 3 - Trellis application components

##SmartCache design
There is a cost to each item placed in a (simple or object) cache. Sometimes, the right conditions are present where this cost can be minimized. SmartCache is a simple cache that minimizes this cost when the following conditions are present:
* The payload (size of key + size of value) is relatively small (<2-5 KB)
* The keys are composed in such a way that several items have the same key up to the last byte (You should use byte[] as keys. The API supports Serializable; however, byte[] are not compressed and essentially kept intact, as is required for this scheme.)

For example, the set of 4 bytes keys illustrated in Figure 4 matches condition 2.
![Figure 4 - Keys matching condition for SmartCache components](https://cloud.githubusercontent.com/assets/7895210/8338049/4bf26200-1a63-11e5-819f-7ca54a884073.jpg)
####Figure 4 - Keys matching condition for SmartCache

If both conditions are met, then significant memory reduction can be obtained. In the example of Figure 4, there are 11 entries, corresponding to 11 entries in SimpleCache. With SmartCache, this will be reduced to only 3 larger entries, where only the first 3 bytes of the key are used as key into the ObjectCache and the fourth byte is used to index into an array internal to the ObjectCache SmartCache object where the values are stored, as illustrated in Figure 5.
 
####[Figure 5 "Splitting of the key for SmartCache" goes here]

SmartCache will implement exactly the ITrellisSimpleCacheClient interface. However, using its own optimized way of storing the data, it will be more memory efficient.
