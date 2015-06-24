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
![Figure 1](https://cloud.githubusercontent.com/assets/7895210/8338052/4bf4f3a8-1a63-11e5-9437-1f857309b363.jpg)
####Figure 1 System overview: Routing
![Figure 2](https://cloud.githubusercontent.com/assets/7895210/8338053/4bf509f6-1a63-11e5-8a0c-1250469902e6.jpg)
####Figure 2 System overview: Client request to cache object
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

Figure 3 illustrates the various components of a typical trellis application. In this section, we will build a complete EchoX3 application named SmartCache (included in the EchoX3 distribution).
![Figure 3](https://cloud.githubusercontent.com/assets/7895210/8338054/4bf62cdc-1a63-11e5-8b8f-1e6b7fbfbe7e.jpg)
####Figure 3 Trellis application components

##SmartCache design
There is a cost to each item placed in a (simple or object) cache. Sometimes, the right conditions are present where this cost can be minimized. SmartCache is a simple cache that minimizes this cost when the following conditions are present:
* The payload (size of key + size of value) is relatively small (<2-5 KB)
* The keys are composed in such a way that several items have the same key up to the last byte (You should use byte[] as keys. The API supports Serializable; however, byte[] are not compressed and essentially kept intact, as is required for this scheme.)

For example, the set of 4 bytes keys illustrated in Figure 4 matches condition 2.

![Figure 4](https://cloud.githubusercontent.com/assets/7895210/8338049/4bf26200-1a63-11e5-819f-7ca54a884073.jpg)
####Figure 4 Keys matching condition for SmartCache

If both conditions are met, then significant memory reduction can be obtained. In the example of Figure 4, there are 11 entries, corresponding to 11 entries in SimpleCache. With SmartCache, this will be reduced to only 3 larger entries, where only the first 3 bytes of the key are used as key into the ObjectCache and the fourth byte is used to index into an array internal to the ObjectCache SmartCache object where the values are stored, as illustrated in Figure 5.
 
![Figure 5](https://cloud.githubusercontent.com/assets/7895210/8338051/4bf488e6-1a63-11e5-992b-0990aadb1697.jpg)
####Figure 5 Splitting of the key for SmartCache

SmartCache will implement exactly the ITrellisSimpleCacheClient interface. However, using its own optimized way of storing the data, it will be more memory efficient.

##SmartCacheObject implements ICacheObject
The core of the application is the SmartCache object. In this case, to be fully compatible with the regular SimpleCache, SmartCache needs to support expiration based on TimeWrite or TimeRead. This means we need to maintain an m_lastWrite and m_lastRead for each entry in addition to the m_data.

##Member variables
The member variables for SmartCacheObject become the array version of the member variables for SimpleCacheObject, as illustrated in Figure 6.

	public class SmartCacheObject implements ITrellisCacheObject
	{
		private TrellisSimpleCacheStatusHolder		m_cacheStatus;

		private byte[][]	m_dataList			= new byte[256][];
		private long[]	m_writeTimeListMS		= new long[256];
		private long[]	m_readTimeListMS		= new long[256];
		private long[]	m_expirationTimeListMS	= new long[256];

	}
####Figure 6 Member variables for SmartCache

The member variable m_cacheStatus matches same in SimpleCache and maintains the configuration data along with counters. Each object has a pointer to the object owned by the factory associated with the named cache.

Appropriate getters are available on each of these member variables so they can be stolen by a later release of SmartCacheObject for in-place upgrade via the method upgradeClass() of the v2 (or v1.1) new object/class.

The readOnly() and writeOnly() method will be dealt with in the section on request below. The remaining ITrellisCacheObject methods are discussed here.

##Constructor
Following the pattern of TrellisSimpleCacheObject, the constructor takes the TrellisSimpleCacheStatusHolder and keeps a pointer to the master owned by the factory object. After final construction, all data is initialized to null.

##void updateConfiguration(TrellisCacheConfiguration configuration)
This method is called on existing objects to inform them that the configuration has changed. The factory is always called first (guaranteed by Trellis). This means that the m_cacheStatus has been updated before this call. The parameter configuration is ignored and m_expirationTimeListMS is updated for the new configuration.

##void flush(long timeNowMS, long timeFlushMS)
Flush can be a dangerous call in a conventional system. The problem is that, immediately after the flush, there are no entries in the cache and every call is a miss. A simplistic application using caching is illustrated in Figure 7.

		Foo		foo	= cache.get(key);
		if (null == foo)
		{
			foo = backend.computeFoo(...);
			cache.put(key, foo);
		}
		// Use the value of foo
####Figure 7 Simple application using cache

After a flush, every call will go to the backend and risks overloading it. To avoid this load spike on the backend, Trellis supports a soft-flush. The flush is spread over N milli-seconds. The client API for flush takes the parameter flushDurationMS. On each server, Trellis utilizes a linear function to distribute the flush time of each object over the flush duration. Trellis then immediately tells each object at which time it should flush itself. It is the responsibility of the object to determine based on its own semantics how it wants to proceed.

In the case of SimpleCache or SmartCache, this is straight forward as the read and write times can be adjusted to ensure the object does not live beyond the scheduled flush time. For each other class of object, the details may be different, up to and including some object that will ignore the call and some object that will ignore the parameter timeFlushMS and flush themselves immediately. That part belongs to the owner (writer/coder) of the object.
 
![Figure 8](https://cloud.githubusercontent.com/assets/7895210/8338050/4bf44db8-1a63-11e5-912b-1093ad150378.jpg)
####Figure 8 Adjusting read/write time for soft flush in SimpleCache and SmartCache

##void doMaintenance(int memoryLevelPercent)
The method doMaintenance is where the object cleans itself. In the case of a SmartCache, it will go through each of its item and delete (nullify) each expired item.

The parameter memoryLevelPercent indicates the level of memory pressure under which the system is currently operating. In other words, this is the percent of normal memory the application should use. When the number is below 100, a well-behaved application should reduce its memory requirement.

In the cache of a standard caching application with a time-to-live (TTL), there is an obvious way to use this parameter. The configuration TTL is multiplied by the memoryLevelPercent (TTL * memoryLevelPercent / 100) to obtain the effective TTL and the effective TTL is used. 

The configuration parameters MaintenancePeriodNumber and MaintenancePeriodUnits are used to configure on a per named cache basis how often this method is called.

##Boolean canDelete()
This method is called after every operation which can modify the object (doMaintenance and writeOnly). Trellis is asking the object is it can be deleted. For SmartCache, the answer is yes when all elements of the data list are null.

For SmartCache, there are two ways of implementing this method
* Walk the list until a non-null is found (return false) or the end of list is found (return true)
* Keep a count of non-null elements and return count == 0

##Client wrapper implements ISimpleCacheClient
The client wrapper acts as a simplification agent between the clients of the EchoX3 application and the EchoX3 system. To its clients, the wrapper exposes a simple interface in clients “units”. For example, the SmartCache exposes a plain cache interface. The implementation details are hidden from the clients. The wrapper takes in requests and translates them into Trellis calls, either to the SimpleCache or to the ObjectCache, as is appropriate. In a more advanced scenario, a wrapper’s single entry point could require multiple calls to perform its task before returning to the caller.
 
![Figure 9](https://cloud.githubusercontent.com/assets/7895210/8338057/4c06ccfe-1a63-11e5-8ecc-8ed4cd153b45.jpg)
####Figure 9 Client wrapper interfaces

For SmartCache, the obvious and simplest approach is to expose the same API as is used for the Trellis SimpleCache: ISimpleCacheClient:

		class SmartCacheClient implements ITrellisSimpleCacheClient

##Request Objects
There are several approaches possible to the complexity of the request objects. Remember that they are full-fledged Java objects that will exist on the server. At the simple end, they are POJO used by the cache object to determine the request and request parameters. At the more complex end, they can contain the algorithm to run on the cache object’s data.
For SmartCache, the simplicity of the problem lends itself towards the POJO end of the spectrum.

##SmartCacheRequest
The plain SmartCacheRequest is used in both Read and Write mode and is the class that understand how to parse the key to extract the bytes corresponding to the object’s key and the byte corresponding to the array index for the element within the cache object.

	public class SmartCacheRequest implements Serializable
	{
		private transient byte[]		m_objectKey;
		private int				m_index;
	}

####Figure 10 Fields of SmartCacheRequest

This class serves multiple purposes as it not only act as a base class for SmartCacheWriteRequest (see below), it is also the class that parses the full key into its components. The components are stored in the two member variables. Note that m_objectKey is marked transient as it does not need to be transmitted to the server. It is used directly as a parameter (key) to the writeOnly (or readOnly) call.

##WriteRequest
The write request extends SmartCacheRequest, adding the byte[] corresponding to the value to be stored (see Figure 11).

	public class SmartCacheWriteRequest extends SmartCacheRequest
	{
		private byte[]		m_value;
	}

####Figure 11 Fields of SmartCacheWriteRequest

##ReadRequest
Given the simplicity of this application, the read request is simply the index of the value to retrieve. The class SmartCacheRequest is used to create the required key components.

##Debugging the application
The first phase of debugging takes place in **ClientType.Local**.

It is straight forward to debug into the client wrapper, up to the call into objectCache.writeOnly(). At this point, you may want to set some breakpoints to see what your code is doing. The following are key entry points:
* ObjectFactory.createObject()
* cacheObject.writeOnly()
* cacheObject.readOnly()

Once you see these methods execute properly, you can look at the other methods on your object (e.g. doMaintenance).

##Architecture overview
Clients are the user application calling entry points into libraries supplied by us. The initial offering will include a Java library . All client libraries will implement the same functionality in remote mode. The same Java client will support both Windows and Linux clients.

The servers will be written entirely in 100% portable Java. They will run on Windows, Mac and Linux. Further, the configuration and automatic scalability system will support major cloud providers. It is expected that the v1 server will run on Windows. Yet, the code will be written with the goal to run on Windows, Mac and Linux operating systems.

##Topography
As illustrated (Figure 12), the servers are organized in groups called “clusters”. Each cluster is completely independent from the other clusters in terms of functionality and scalability. The major link between the various “connected” clusters is that, by being aware of each other, it is possible for a client to obtain access to any cluster after connecting to a single server in any cluster.
 
![Figure 12](https://cloud.githubusercontent.com/assets/7895210/8338055/4c0630dc-1a63-11e5-9865-265d55dff887.jpg)
####Figure 12 System overview

At startup, a client only needs to connect to a single server belonging to any cluster of “the system”. It asks that server which servers are members of the cluster containing cache “X”. Any of the permanent servers can answer that question. The client then connects to the servers and is then connected directly to the cluster and independent of any other cluster within the enterprise system.

Internally, each server is composed of three somewhat independent components:
* Transport . On the receiving end, this component receives requests, posts them to the appropriate thread pool. On the other side of the transactions, this component is responsible for transmitting data to other components (e.g. responses to clients or requests to other servers)
* A dispatcher. The director receives requests from clients and is responsible for applying the appropriate routing table to obtain the response required by the client.
* A local ObjectCache responsible for managing the various named ObjectCache stored on the server.

A direct implication of 2 above is that the system is a 2 hops system. This has a cost and provides benefits. The obvious cost is performance. Try the system and, if you find it too slow, tell us and we will fix it. All performance tests performed on our system (e.g. prototype) shows that it is as fast as the fastest commercial single hop system and it is much faster than most.

The 2 hops approach has several advantages, including
* Changes to the back-end (anything from OS maintenance requiring a server bounce to a major version upgrade of the backend) can be made transparently to the client.
* A much higher scalability can be achieved.

##Simplified client interface
The client interface is extremely simple, by design…

The first entry point is used to obtain a client instance object. Note that the instance is a singleton. There are two singleton within the system, one for local and one for remote. In a typical scenario, one may choose to use the local mode for early debugging and switch to remote (100% compatible) for integrated testing and production. In a more complex scenario, both may be used for different named objects. Of course, the use of both concurrently is supported.
* Factory.getInstance().getSimpleClient (Boolean isLocal)
    or
    Factory.getInstance().getObjectClient (Boolean isLocal)

The next call tells the system which named cache you want to use. Multiple calls can be made to access multiple named caches.
* connectToCache(String name)

Then the system is ready to be used with SimpleCache calls such as …
* void put(String name, Serializable key, Serializable value)
* Serializable get(String name, Serializable value)

or ObjectCache calls such as
* boolean writeOnly(String name, Serializable key, Serializable request)
* Serializable readOnly(String name, Serializable key, Serializable request)

Of course, all the appropriate bulk flavors of these calls are available in the complete API. The API will also include other utility entry points known to be useful in such systems.
