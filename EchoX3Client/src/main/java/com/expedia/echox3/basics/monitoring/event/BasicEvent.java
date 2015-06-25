/**
 * Copyright 2013-2014 Expedia, Inc. All rights reserved.
 * EXPEDIA PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package com.expedia.echox3.basics.monitoring.event;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class BasicEvent implements Serializable
{
	public static final long					serialVersionUID	= 20150601085959L;

	private static final Map<Integer, BasicEvent> MAP		= new HashMap<>();

	// Range reserved for Ops defined numbers	    0 -   100
	// Range reserved for Basics system:		40000 - 40899
	// Range reserved for Worker system:		41000 - 41999
	// Range reserved for Client system:		43000 - 43999
	// Range reserved for CLI system:			44000 - 44999
	// Range reserved for GridComponents:		45000 - 45999
	// Range reserved for WebFrame:				49000 - 49099
	// Range reserved for Forklift web:			49100 - 49199
	// Range reserved for FrontDoor web:		49200 - 49299

	//CHECKSTYLE:OFF
	// Range reserved for Ops defined numbers	    0 -   100
	public static final BasicEvent	EVENT_SUCCESS									= new BasicEvent(    0, "Success");
	public static final BasicEvent	EVENT_START_COMPLETE							= new BasicEvent(    1, "Start complete");
	public static final BasicEvent	EVENT_SHUTDOWN									= new BasicEvent(    2, "Shutdown");
	public static final BasicEvent	EVENT_TODO										= new BasicEvent(   10, "TODO");
	public static final BasicEvent	EVENT_DEBUG										= new BasicEvent(   15, "Debug");
	public static final BasicEvent	EVENT_TEST										= new BasicEvent(   20, "Test");
	public static final BasicEvent	EVENT_COUNTER									= new BasicEvent(   25, "ChangeToCounter");

	// 1xx Reserved for generic events
	public static final BasicEvent	EVENT_UTF8									= new BasicEvent(  100, "UTF8");
	public static final BasicEvent	EVENT_BASE64								= new BasicEvent(  101, "BasicBase64");
	public static final BasicEvent	EVENT_TOOLS_MBEAN_NAME_ERROR				= new BasicEvent(  101, "BasicMBeanNameError");

	public static final BasicEvent	EVENT_OBJECT_POOL_MAX_SIZE					= new BasicEvent(  110, "ObjectPoolMaxSize");
	public static final BasicEvent	EVENT_OBJECT_POOL_GROW						= new BasicEvent(  111, "ObjectPoolGrow");
	public static final BasicEvent	EVENT_OBJECT_POOL_SHRINK					= new BasicEvent(  112, "ObjectPoolShrink");

	// 101x Reserved for counters
	public static final BasicEvent	EVENT_COUNTER_CREATE						= new BasicEvent( 1010, "CounterCreate");
	public static final BasicEvent	EVENT_COUNTER_HISTOGRAM						= new BasicEvent( 1011, "CounterHistogram");
	public static final BasicEvent	EVENT_COUNTER_ACCUMULATOR					= new BasicEvent( 1012, "CounterAccumulator");
	public static final BasicEvent	EVENT_COUNTER_INCREMENT						= new BasicEvent( 1013, "CounterIncrement");
	public static final BasicEvent	EVENT_COUNTER_RESOURCE						= new BasicEvent( 1014, "CounterResource");

	// 1150-1199 reserved for Serial/ClassLoader
	public static final BasicEvent	EVENT_SERIAL_JAVA_SERIAL_FAILED				= new BasicEvent( 1150, "SerializationFailed");
	public static final BasicEvent	EVENT_SERIAL_COMPRESS_FAILED				= new BasicEvent( 1151, "CompressionFailed");
	public static final BasicEvent	EVENT_SERIAL_UNKNOWN_COMPRESSION			= new BasicEvent( 1152, "UnknownCompression");
	public static final BasicEvent	EVENT_SERIAL_UNKNOWN_SERIALIZATION			= new BasicEvent( 1153, "UnknownSerialization");
	public static final BasicEvent	EVENT_SERIAL_JAVA_DESERIAL_FAILED			= new BasicEvent( 1150, "de-SerializationFailed");
	public static final BasicEvent	EVENT_SERIAL_DECOMPRESS_FAILED				= new BasicEvent( 1151, "de-CompressionFailed");
	public static final BasicEvent	EVENT_CLASS_LOADER_CLASS_NOT_FOUND			= new BasicEvent( 1160, "ClassNotFound");
	public static final BasicEvent	EVENT_CLASS_LOADER_DEFINE_CLASS_FAILED		= new BasicEvent( 1161, "DefineClassFailed");
	public static final BasicEvent	EVENT_CLASS_LOADER_INCOMPATIBLE_DEFINITION	= new BasicEvent( 1162, "IncompatibleClassDef");
	public static final BasicEvent	EVENT_CLASS_LOADER_DEPENDENCY_RESOLUTION	= new BasicEvent( 1163, "DependencyResolutionFailed");

	// 1200-1299 reserved for basic transport
	public static final BasicEvent	EVENT_SELECTOR_OPEN_FAILED					= new BasicEvent( 1200, "SelectorOpenFailed");
	public static final BasicEvent	EVENT_SELECTOR_EVENT_ACCEPTABLE				= new BasicEvent( 1201, "SelectorIsAcceptable");
	public static final BasicEvent	EVENT_SELECTOR_EVENT_CONNECTABLE			= new BasicEvent( 1202, "SelectorIsConnectable");
	public static final BasicEvent	EVENT_SELECTOR_EVENT_WRITABLE				= new BasicEvent( 1203, "SelectorIsWritable");
	public static final BasicEvent	EVENT_SELECTOR_EVENT_READABLE				= new BasicEvent( 1204, "SelectorIsReadable");
	public static final BasicEvent	EVENT_SELECTOR_EVENT_EXCEPTION				= new BasicEvent( 1205, "SelectorException");
	public static final BasicEvent	EVENT_KEY_REGISTER_EXCEPTION				= new BasicEvent( 1206, "KeyRegisterException");
	public static final BasicEvent	EVENT_KEY_CHANGE_EXCEPTION					= new BasicEvent( 1207, "KeyChangeException");

	public static final BasicEvent	EVENT_SOCKET_EXCEPTION_READ					= new BasicEvent( 1210, "SocketExceptionRead");
	public static final BasicEvent	EVENT_SOCKET_EXCEPTION_WRITE				= new BasicEvent( 1211, "SocketExceptionWrite");

	public static final BasicEvent	EVENT_SOCKET_CLOSING						= new BasicEvent( 1218, "SocketClose");
	public static final BasicEvent	EVENT_SOCKET_CLOSED							= new BasicEvent( 1219, "SocketAlreadyClosed");

	public static final BasicEvent	EVENT_SOCKET_SERVER_OPEN_SUCCESS			= new BasicEvent( 1220, "SocketServerOpen");
	public static final BasicEvent	EVENT_SOCKET_SERVER_OPEN_FAIL				= new BasicEvent( 1220, "SocketServerFail");
	public static final BasicEvent	EVENT_SOCKET_SERVER_CONNECT					= new BasicEvent( 1221, "SocketServerConnect");
	public static final BasicEvent	EVENT_SOCKET_EXCEPTION_ACCEPT				= new BasicEvent( 1222, "SocketExceptionAccept");
	public static final BasicEvent	EVENT_SOCKET_CONDITION_FAILED				= new BasicEvent( 1223, "SocketConditionFailed");

	public static final BasicEvent	EVENT_SOCKET_CLIENT_OPEN_PENDING			= new BasicEvent( 1230, "SocketClientOpenPending");
	public static final BasicEvent	EVENT_SOCKET_CLIENT_OPEN_SUCCESS			= new BasicEvent( 1231, "SocketClientOpenSuccess");
	public static final BasicEvent	EVENT_SOCKET_CLIENT_OPEN_FAILURE			= new BasicEvent( 1232, "SocketClientOpenFailure");
	public static final BasicEvent	EVENT_SOCKET_EXCEPTION_CONNECT				= new BasicEvent( 1233, "SocketExceptionConnect");
	public static final BasicEvent	EVENT_SOCKET_EXCEPTION_RESOLVE				= new BasicEvent( 1234, "SocketExceptionResolve");
	public static final BasicEvent	EVENT_SOCKET_CONFIGURE						= new BasicEvent( 1235, "SocketConfigure");

	public static final BasicEvent	EVENT_SOURCE_PROTOCOL_RECALCULATE			= new BasicEvent( 1240, "SourceProtocolRecalculate");
	public static final BasicEvent	EVENT_SOURCE_PROTOCOL_SHOW_LIST				= new BasicEvent( 1241, "SourceProtocolShowList");

	public static final BasicEvent	EVENT_SOCKET_PROTOCOL_CHECKSUM				= new BasicEvent( 1250, "ProtocolChecksum");
	public static final BasicEvent	EVENT_PROTOCOL_PARSE_ERROR					= new BasicEvent( 1251, "ProtocolParseRequestFail");
	public static final BasicEvent	EVENT_MESSAGE_PROCESSING_ERROR				= new BasicEvent( 1252, "MessageProcessingFail");
	public static final BasicEvent	EVENT_MESSAGE_TYPE_UNKNOWN					= new BasicEvent( 1253, "MessageTypeUnknown");

	public static final BasicEvent	EVENT_TRANSPORT_KEEP_ALIVE_WAKEUP			= new BasicEvent( 1280, "KeepAliveWakeup");
	public static final BasicEvent	EVENT_TRANSPORT_KEEP_ALIVE_LANE_STATUS		= new BasicEvent( 1281, "KeepAliveLaneStatus");
	public static final BasicEvent	EVENT_TRANSPORT_LANE_STATUS_CHANGE			= new BasicEvent( 1285, "TransportLaneStatusChange");
	public static final BasicEvent	EVENT_TRANSPORT_LANE_JOIN_HIGHWAY			= new BasicEvent( 1286, "TransportLaneJoinHighway");
	public static final BasicEvent	EVENT_TRANSPORT_LANE_CLIENT_READY			= new BasicEvent( 1287, "TransportLaneClientReady");
	public static final BasicEvent	EVENT_TRANSPORT_LANE_NOT_AVAILABLE			= new BasicEvent( 1288, "TransportLaneNotAvailable");

	public static final BasicEvent	EVENT_PROTOCOL_TIMEOUT						= new BasicEvent( 1290, "ProtocolTimeout");
	public static final BasicEvent	EVENT_PROTOCOL_SHUTDOWN						= new BasicEvent( 1291, "ProtocolShutdown");
	public static final BasicEvent	EVENT_PROTOCOL_PROCESSING_EXCEPTION			= new BasicEvent( 1292, "ProtocolProcessingException");
	public static final BasicEvent	EVENT_PROTOCOL_GET_VERSION_ERROR			= new BasicEvent( 1295, "GetVersionError");

	// 1300-1349 Reserved for Message handlers
	public static final BasicEvent	EVENT_MESSAGE_HANDLER_BEGIN					= new BasicEvent( 1400, "MessageHandlerListenBegin");
	public static final BasicEvent	EVENT_MESSAGE_HANDLER_FAIL					= new BasicEvent( 1401, "MessageHandlerListenFail");
	public static final BasicEvent	EVENT_MESSAGE_HANDLER_STOP					= new BasicEvent( 1402, "MessageHandlerListenStop");

	public static final BasicEvent	EVENT_PROTOCOL_SEND_REQUEST					= new BasicEvent( 1411, "ProtocolSendRequest");
	public static final BasicEvent	EVENT_PROTOCOL_RECEIVE_REQUEST				= new BasicEvent( 1412, "ProtocolReceiveRequest");
	public static final BasicEvent	EVENT_PROTOCOL_SEND_RESPONSE				= new BasicEvent( 1413, "ProtocolSendResponse");
	public static final BasicEvent	EVENT_PROTOCOL_RECEIVE_RESPONSE				= new BasicEvent( 1413, "ProtocolReceiveResponse");

	// 1500-1549 Reserved for Crypto
	public static final BasicEvent	EVENT_NO_SECURE_RANDOM_ALGORITHM			= new BasicEvent( 1500, "NoSecureRandomAlgorithm");
	public static final BasicEvent	EVENT_PASSWORD_INVALID_PARAMETER			= new BasicEvent( 1501, "PasswordInvalidParameter");
	public static final BasicEvent	EVENT_PASSWORD_FAILED						= new BasicEvent( 1502, "PasswordFailed");
	public static final BasicEvent	EVENT_CRYPTO_PASSWORD_INIT_FAILED			= new BasicEvent( 1510, "CryptoPasswordInitFailed");
	public static final BasicEvent	EVENT_CRYPTO_PASSWORD_ENCRYPT_FAILED		= new BasicEvent( 1511, "CryptoPasswordEncryptFailed");
	public static final BasicEvent	EVENT_CRYPTO_PASSWORD_DECRYPT_FAILED		= new BasicEvent( 1512, "CryptoPasswordDecryptFailed");
	public static final BasicEvent	EVENT_CRYPTO_UNKNOWN_ALIAS					= new BasicEvent( 1513, "CryptoUnknownAlias");
	public static final BasicEvent	EVENT_KEYSTORE_LOAD_FAILED					= new BasicEvent( 1521, "CryptoKeyStoreLoadFailed");
	public static final BasicEvent	EVENT_KEYSTORE_SAVE_SUCCESS					= new BasicEvent( 1522, "CryptoKeyStoreSaveSuccess");
	public static final BasicEvent	EVENT_KEYSTORE_SAVE_FAILED					= new BasicEvent( 1523, "CryptoKeyStoreSaveFailed");

	// 2000-2199 Reserved for local store
	public static final BasicEvent	EVENT_CACHE_CREATE							= new BasicEvent( 2000, "ObjectCache created");
	public static final BasicEvent	EVENT_INCOMPATIBLE_CACHE					= new BasicEvent( 2001, "Mismatched cache name");
	public static final BasicEvent	EVENT_INCOMPATIBLE_CACHE_TYPE				= new BasicEvent( 2002, "Incompatible cache type");
	public static final BasicEvent	EVENT_UNKNOWN_CACHE							= new BasicEvent( 2003, "Cache not found");
	public static final BasicEvent	EVENT_NO_FACTORY_CONSTRUCTOR				= new BasicEvent( 2004, "No factory constructor");
	public static final BasicEvent	EVENT_FACTORY_CHANGE						= new BasicEvent( 2005, "Factory changed");
	public static final BasicEvent	EVENT_CACHE_COUNTERS_STATUS					= new BasicEvent( 2006, "CounterStatus");
	public static final BasicEvent	EVENT_PROTECTION_OOM_PERCENT_LEVEL			= new BasicEvent( 2007, "OomPercentLevel");
	public static final BasicEvent	EVENT_PROTECTION_OOM						= new BasicEvent( 2008, "OomLevelExceeded");
	public static final BasicEvent	EVENT_CACHE_CLOSE							= new BasicEvent( 2019, "ObjectCache closed");

	public static final BasicEvent	EVENT_CACHE_MAINTENANCE_PASS				= new BasicEvent( 2020, "MaintenancePass");
	public static final BasicEvent	EVENT_CACHE_MAINTENANCE_CHANGE				= new BasicEvent( 2021, "Maintenance period changed");
	public static final BasicEvent	EVENT_CACHE_EXPIRATION_CHANGE				= new BasicEvent( 2022, "Expiration changed");
	public static final BasicEvent	EVENT_FLUSH_BEGIN							= new BasicEvent( 2025, "Flush begin");
	public static final BasicEvent	EVENT_FLUSH_INTERNAL_PAUSE					= new BasicEvent( 2026, "Flush pause");
	public static final BasicEvent	EVENT_FLUSH_COMPLETE						= new BasicEvent( 2027, "Flush complete");

	public static final BasicEvent	EVENT_CACHE_OBJECT_EXCEPTION_CREATE			= new BasicEvent( 2030, "ObjectException Create");
	public static final BasicEvent	EVENT_CACHE_OBJECT_EXCEPTION_WRITE			= new BasicEvent( 2031, "ObjectException Write");
	public static final BasicEvent	EVENT_CACHE_OBJECT_EXCEPTION_READ			= new BasicEvent( 2032, "ObjectException Read");
	public static final BasicEvent	EVENT_CACHE_OBJECT_EXCEPTION_MAINTENANCE	= new BasicEvent( 2033, "ObjectException Maintenance");
	public static final BasicEvent	EVENT_CACHE_OBJECT_EXCEPTION_UPGRADE		= new BasicEvent( 2034, "ObjectException Upgrade");

	public static final BasicEvent	EVENT_BUCKET_COUNT_CHANGE					= new BasicEvent( 2040, "Bucket count changed");
	public static final BasicEvent	EVENT_BIN_COUNT_CHANGE						= new BasicEvent( 2045, "Bin count changed");

	public static final BasicEvent	EVENT_VERSION_LOAD_FAILED					= new BasicEvent( 2050, "VersionLoadFailed");

	// 2990-2999 Reserved for TrellisClientWrapper
	public static final BasicEvent	EVENT_WRAPPER_TYPE_CHANGED					= new BasicEvent( 2990, "WrapperTypeChange");
	public static final BasicEvent	EVENT_WRAPPER_TYPE_UNKNOWN					= new BasicEvent( 2991, "WrapperTypeUnknown");
	public static final BasicEvent	EVENT_WRAPPER_CONNECT_FAILED				= new BasicEvent( 2992, "WrapperConnectFailed");
	public static final BasicEvent	EVENT_WRAPPER_CLOSE_FAILED					= new BasicEvent( 2993, "WrapperCloseFailed");

	// Events 9xxx Reserved for subsystem Command...
	// Events 9000 - 9050 reserved for Firetruck
	public static final BasicEvent	EVENT_FIRETRUCK_RUN_ONCE_SUCCESS			= new BasicEvent( 9000, "FiretruckSingleRunning");
	public static final BasicEvent	EVENT_FIRETRUCK_RUN_ONCE_EXCEPTION			= new BasicEvent( 9001, "FiretruckSingleFailing");


	// Range reserved for Basics system:		40000 - 40899
	// 40000-40099 Reserved for Small groups (assigned in chunks of 10
	// 4000x Reserved for Manifest/version
	public static final BasicEvent	EVENT_MANIFEST_GET_VERSION_ERROR				= new BasicEvent(40000, "ManifestGetVersion");
	public static final BasicEvent	EVENT_DATABASE_LOAD_MANIFEST_EXCEPTION			= new BasicEvent(40001, "LoadManifestException");

	// 4001x Reserved for WallClock
	public static final BasicEvent	EVENT_WALL_CLOCK_TIME_ZONE						= new BasicEvent(40010, "WallClockTimeZone");
	public static final BasicEvent	EVENT_WALL_CLOCK_CONFIGURATION_CHANGE			= new BasicEvent(40011, "WallClockConfigurationChange");

	public static final BasicEvent	EVENT_MASTER_CLOCK_EXCEPTION					= new BasicEvent(40015, "MasterClockException");

	public static final BasicEvent	EVENT_PRIME_NUMBERS_FOUND						= new BasicEvent(40020, "FoundPrimeNumbers");
	public static final BasicEvent	EVENT_PRIME_NUMBERS_GENERATED					= new BasicEvent(40021, "GeneratedPrimeNumbers");
	public static final BasicEvent	EVENT_PRIME_NUMBERS_READ						= new BasicEvent(40022, "ReadPrimeNumbers");

	// 4009x reserved for odds & ends
	public static final BasicEvent	EVENT_TIME_TRAVELER								= new BasicEvent(40090, "TimeTraveler");
	public static final BasicEvent	EVENT_DATABASE_NO_DEFAULT_CONSTRUCTOR_FOR_POOL	= new BasicEvent(40091, "NoDefaultConstructorForPooledObject");

	// 40100-40124 Reserved for AbstractBasicThread & family
	public static final BasicEvent	EVENT_THREAD_SCHEDULE							= new BasicEvent(40101, "ThreadSchedule");
	public static final BasicEvent	EVENT_THREAD_STARTING							= new BasicEvent(40102, "ThreadStarting");
	public static final BasicEvent	EVENT_THREAD_EXITING							= new BasicEvent(40103, "ThreadExiting");
	public static final BasicEvent	EVENT_THREAD_SCHEDULE_INVALID					= new BasicEvent(40104, "ThreadScheduleInvalid");
	public static final BasicEvent	EVENT_THREAD_SCHEDULE_CHANGE					= new BasicEvent(40105, "ThreadScheduleChange");
	public static final BasicEvent	EVENT_THREAD_EXCEPTION							= new BasicEvent(40106, "ThreadException");

	// 40125-40149 Reserved for EventPublisher
	public static final BasicEvent	EVENT_PUBLISHER_THREAD_POOL_CREATE				= new BasicEvent(40125, "PublisherPoolCreate");
	public static final BasicEvent	EVENT_PUBLISHER_THREAD_POOL_ASSIGN				= new BasicEvent(40126, "PublisherPoolAssign");
	public static final BasicEvent	EVENT_PUBLISHER_THREAD_POOL_MISSING				= new BasicEvent(40127, "PublisherPoolMissing");
	public static final BasicEvent	EVENT_PUBLISHER_EVENT_EXCEPTION_BEGIN			= new BasicEvent(40128, "EventExceptionBegin");
	public static final BasicEvent	EVENT_PUBLISHER_LISTENER_EXCEPTION				= new BasicEvent(40129, "ListenerException");
	public static final BasicEvent	EVENT_PUBLISHER_EVENT_EXCEPTION_END				= new BasicEvent(40130, "EventExceptionEnd");

	// 40150-40174 Reserved for BaseFileHandler & family (aka io subsystem)
	public static final BasicEvent	EVENT_FILE_FINDER_NOT_FOUND						= new BasicEvent(40150, "FileNotFound");
	public static final BasicEvent	EVENT_SYSTEM_EXEC_FAILED						= new BasicEvent(40151, "SystemExecFailed");
	public static final BasicEvent	EVENT_FILE_OPEN_FAILED							= new BasicEvent(40152, "FileOpenFailed");
	public static final BasicEvent	EVENT_FILE_CREATE_FAILED						= new BasicEvent(40153, "FileCreateFailed");
	public static final BasicEvent	EVENT_FILE_OPEN_FOR_WRITE_FAILED				= new BasicEvent(40154, "FileOpenForWriteFailed");
	public static final BasicEvent	EVENT_PRIME_WRITE_FAILED						= new BasicEvent(40155, "PrimeWriteFailed");
	public static final BasicEvent	EVENT_PRIME_READ_FAILED							= new BasicEvent(40156, "PrimeReadFailed");
	public static final BasicEvent	EVENT_INVALID_URL								= new BasicEvent(40157, "InvalidURL");
	public static final BasicEvent	EVENT_JAR_READ_ERROR							= new BasicEvent(40158, "JarReadError");
	public static final BasicEvent	EVENT_FOLDER_REGISTER_FAILED					= new BasicEvent(40159, "FolderRegisterFailed");
	public static final BasicEvent	EVENT_URL_CONFIGURATION_CACHE_WRITE_FAILED		= new BasicEvent(40160, "UrlConfigurationCacheWriteFailed");
	public static final BasicEvent	EVENT_URL_CONFIGURATION_CACHE_READ_FAILED		= new BasicEvent(40161, "UrlConfigurationCacheReadFailed");

	// 40175-40199 reserved for Configuration
	public static final BasicEvent	EVENT_CONFIGURATION_CHANGE						= new BasicEvent(40175, "ConfigurationChange");
	public static final BasicEvent	EVENT_CONFIGURATION_LOAD_CLASSPATH				= new BasicEvent(40176, "ConfigurationLoadClasspath");
	public static final BasicEvent	EVENT_CONFIGURATION_LOAD_FOLDER					= new BasicEvent(40177, "ConfigurationLoadFolder");
	public static final BasicEvent	EVENT_CONFIGURATION_PARSE_ERROR					= new BasicEvent(40178, "ConfigurationParseError");
	public static final BasicEvent	EVENT_CONFIGURATION_CP_ROOT_FOLDER_READ_ERROR	= new BasicEvent(40179, "ConfigurationClassPathRootFolderReadError");
	public static final BasicEvent	EVENT_CONFIGURATION_CP_FILE_RELOAD_ERROR		= new BasicEvent(40180, "ConfigurationClassPathFileReloadError");
	public static final BasicEvent	EVENT_CONFIGURATION_FILE_LOAD_ERROR				= new BasicEvent(40181, "ConfigurationFileLoadError");
	public static final BasicEvent	EVENT_CONFIGURATION_URL_LOAD_ERROR				= new BasicEvent(40182, "ConfigurationUrlLoadError");
	public static final BasicEvent	EVENT_CONFIGURATION_FOLDER_LIST_ERROR			= new BasicEvent(40183, "ConfigurationFolderListError");
	public static final BasicEvent	EVENT_CONFIGURATION_FOLDER_WATCHER_INIT_ERROR	= new BasicEvent(40184, "ConfigurationFolderWatcherInitError");
	public static final BasicEvent	EVENT_CONFIGURATION_FOLDER_WATCHER_RUN_ERROR	= new BasicEvent(40185, "ConfigurationFolderWatcherRunError");
	public static final BasicEvent	EVENT_CONFIGURATION_URL_INVALID					= new BasicEvent(40186, "ConfigurationUrlInvalid");

	// 40200-40224 reserved for BasicMBeanProxy
	public static final BasicEvent	EVENT_MBEAN_CREATE								= new BasicEvent(40200, "JMXCreateConnection");
	public static final BasicEvent	EVENT_MBEAN_CONNECT								= new BasicEvent(40201, "JMXConnectSuccess");
	public static final BasicEvent	EVENT_MBEAN_CONNECT_FAILED						= new BasicEvent(40202, "Unable to connect");
	public static final BasicEvent	EVENT_MBEAN_DISCONNECT							= new BasicEvent(40203, "JMXDisconnect");
	public static final BasicEvent	EVENT_MBEAN_CONNECT_ERROR						= new BasicEvent(40204, "Unable to connect");
	public static final BasicEvent	EVENT_MBEAN_GET_OBJECT_ERROR					= new BasicEvent(40205, "MBean get object error");
	public static final BasicEvent	EVENT_MBEAN_COUNTER_NOT_FOUND					= new BasicEvent(40206, "MBean counter not found");
	public static final BasicEvent	EVENT_MBEAN_OPERATION_ERROR						= new BasicEvent(40207, "MBean operation failed");
	public static final BasicEvent	EVENT_MBEAN_SELECT_ERROR						= new BasicEvent(40208, "MBean select error");

	// 40225-40249 reserved for JMX Measure objects
	public static final BasicEvent	EVENT_JMX_MEASURE_EXCEPTION						= new BasicEvent(40225, "JMXGetException");
	public static final BasicEvent	EVENT_JMX_JAVA_PARSING_EXCEPTION				= new BasicEvent(40226, "JMXJavaParsingException");
	public static final BasicEvent	EVENT_JMX_MEMORY_PARSING_EXCEPTION				= new BasicEvent(40227, "JMXMemoryParsingException");
//	public static final BasicEvent	EVENT_JMX_CPU_PARSING_EXCEPTION					= new BasicEvent(40228, "JMXCpuParsingException");
	public static final BasicEvent	EVENT_JMX_INPUT_MODULE_PARSING_EXCEPTION		= new BasicEvent(40229, "JMXInputModuleParsingException");

	// 40250-40274 reserved for AbstractDatabaseHandler
	public static final BasicEvent	EVENT_DATABASE_BAD_PASSWORD						= new BasicEvent(40250, "BadPassword");
	public static final BasicEvent	EVENT_DATABASE_FAILED_QUERY						= new BasicEvent(40251, "FailedQuery");
	public static final BasicEvent	EVENT_DATABASE_NO_RESPONSE_CODE					= new BasicEvent(40252, "NoResponseCode");
	public static final BasicEvent	EVENT_DATABASE_FAILED_CLOSE						= new BasicEvent(40253, "FailedClose");
	public static final BasicEvent	EVENT_DATABASE_FAILED_PARSE						= new BasicEvent(40254, "FailedParse");
	public static final BasicEvent	EVENT_DATABASE_FAILED_PREPARE					= new BasicEvent(40255, "FailedPrepare");
	public static final BasicEvent	EVENT_DATABASE_FAILED_CALLABLE					= new BasicEvent(40256, "FailedCallable");
	public static final BasicEvent	EVENT_DATABASE_WRITE_SPROC_CHANGE				= new BasicEvent(40257, "WriteSprocSetChanged");
	public static final BasicEvent	EVENT_DATABASE_WRITE_SPROC_EXCEPTION			= new BasicEvent(40258, "WriteSprocException");


	// 40275-40299 reserved for ThreadPool(s)
	public static final BasicEvent	EVENT_THREAD_POOL_CONFIGURATION_CHANGE			= new BasicEvent(40275, "ThreadPoolConfigChange");
	public static final BasicEvent	EVENT_THREAD_POOL_CONFIGURATION_EXCEPTION		= new BasicEvent(40276, "ThreadPoolConfigException");
	public static final BasicEvent	EVENT_THREAD_POOL_UNEXPECTED_EXCEPTION			= new BasicEvent(40280, "ThreadPoolUnexpectedException");

	// 40300-40349 reserved for DispatcherDirector & family
	public static final BasicEvent	EVENT_DISPATCHER_REQUIRES_MASTER				= new BasicEvent(40300, "DispatcherRequiresMaster");
	public static final BasicEvent	EVENT_DISPATCHER_NOT_ON_MASTER					= new BasicEvent(40301, "DispatcherNotOnMaster");
	public static final BasicEvent	EVENT_DISPATCHER_MASTER_STAND_DOWN				= new BasicEvent(40302, "DispatcherMasterStandDown");
	public static final BasicEvent	EVENT_DISPATCHER_MASTER_COLLISION				= new BasicEvent(40303, "DispatcherMasterCollision");
	public static final BasicEvent	EVENT_DISPATCHER_IS_SOURCE_MASTER				= new BasicEvent(40304, "DispatcherIsSourceMaster");
	public static final BasicEvent	EVENT_DISPATCHER_ROGUE_MASTER_FAILED			= new BasicEvent(40305, "DispatcherRogueMasterFailed");

	public static final BasicEvent	EVENT_DISPATCHER_SCHEDULE						= new BasicEvent(40305, "DispatcherSchedule");
	public static final BasicEvent	EVENT_DISPATCHER_CURRENT_ACTION					= new BasicEvent(40306, "DispatcherCurrentAction");

	public static final BasicEvent	EVENT_DISPATCHER_WAKEUP_DEBUG_ACTION			= new BasicEvent(40310, "DispatcherWakeupAction");
	public static final BasicEvent	EVENT_DISPATCHER_WAKEUP_TO_SLAVE				= new BasicEvent(40311, "DispatcherWakeupToSlave");
	public static final BasicEvent	EVENT_DISPATCHER_WAKEUP_TO_WANNABE				= new BasicEvent(40312, "DispatcherWakeupToSlave");
	public static final BasicEvent	EVENT_DISPATCHER_WAKEUP_REGISTER_FAILED			= new BasicEvent(40313, "DispatcherWakeupRegisterFailed");

	public static final BasicEvent	EVENT_DISPATCHER_SLAVE_DEBUG_ACTION				= new BasicEvent(40315, "DispatcherSlaveAction");
	public static final BasicEvent	EVENT_DISPATCHER_SLAVE_TRANSITION				= new BasicEvent(40316, "DispatcherSlaveTransition");
	public static final BasicEvent	EVENT_DISPATCHER_WANNABE_DEBUG_ACTION			= new BasicEvent(40320, "DispatcherWannabeAction");
	public static final BasicEvent	EVENT_DISPATCHER_WANNABE_RESOLVE_PROCESSING		= new BasicEvent(40321, "DispatcherWannabeResolveProcessing");
	public static final BasicEvent	EVENT_DISPATCHER_WANNABE_RESOLVE_WITH			= new BasicEvent(40322, "DispatcherWannabeResolveWith");
	public static final BasicEvent	EVENT_DISPATCHER_WANNABE_RESOLVE_ERROR			= new BasicEvent(40323, "DispatcherWannabeResolveError");
	public static final BasicEvent	EVENT_DISPATCHER_MODE_CHANGE					= new BasicEvent(40325, "DispatcherChangingMode");
	public static final BasicEvent	EVENT_DISPATCHER_BECOMING_MASTER				= new BasicEvent(40326, "DispatcherBecomingMaster");
	public static final BasicEvent	EVENT_DISPATCHER_WAITING_DEBUG_ACTION			= new BasicEvent(40327, "DispatcherWaitingction");
	public static final BasicEvent	EVENT_DISPATCHER_DONE_WAITING_DEBUG_ACTION		= new BasicEvent(40328, "DispatcherDoneWaitingction");
	public static final BasicEvent	EVENT_DISPATCHER_MASTER_DEBUG_ACTION			= new BasicEvent(40329, "DispatcherMasterAction");

	public static final BasicEvent	EVENT_ZONE_DISPATCHER_SENDING					= new BasicEvent(40330, "ZoneDispatcherSending");
	public static final BasicEvent	EVENT_ZONE_DISPATCHER_SEND_FAILED				= new BasicEvent(40330, "ZoneDispatcherSendFailed");

	public static final BasicEvent	EVENT_ZONE_DISPATCHER_REGISTER					= new BasicEvent(40400, "StorageManagerRegister");
	public static final BasicEvent	EVENT_STORAGE_MANAGER_REGISTER					= new BasicEvent(40425, "StorageManagerRegister");

	// Range reserved for WebFrame:				49000 - 49099
	// NOTE: These two IDs MUST be easy to identify. Other IDs should start at 40900.
	public static final BasicEvent	EVENT_WEB_USER_NOTIFIED_OF_ERROR				= new BasicEvent(49098, "WebUserNotifiedOfError");
	public static final BasicEvent	EVENT_WEB_UNEXPECTED_EXCEPTION					= new BasicEvent(49099, "WebUnexpectedException");

	//CHECKSTYLE:ON

	private int m_code;
	private String m_name;

	public BasicEvent(int code, String name)
	{
		m_code = code;
		m_name = name;

		MAP.put(code, this);
	}

	public int getCode()
	{
		return m_code;
	}

	public String getName()
	{
		return m_name;
	}

	public static BasicEvent get(int index)
	{
		return MAP.get(index);
	}

	/**
	 * Indicates whether some other object is "equal to" this one.
	 * <p>
	 * The {@code equals} method implements an equivalence relation
	 * on non-null object references:
	 * <ul>
	 * <li>It is <i>reflexive</i>: for any non-null reference value
	 * {@code x}, {@code x.equals(x)} should return
	 * {@code true}.
	 * <li>It is <i>symmetric</i>: for any non-null reference values
	 * {@code x} and {@code y}, {@code x.equals(y)}
	 * should return {@code true} if and only if
	 * {@code y.equals(x)} returns {@code true}.
	 * <li>It is <i>transitive</i>: for any non-null reference values
	 * {@code x}, {@code y}, and {@code z}, if
	 * {@code x.equals(y)} returns {@code true} and
	 * {@code y.equals(z)} returns {@code true}, then
	 * {@code x.equals(z)} should return {@code true}.
	 * <li>It is <i>consistent</i>: for any non-null reference values
	 * {@code x} and {@code y}, multiple invocations of
	 * {@code x.equals(y)} consistently return {@code true}
	 * or consistently return {@code false}, provided no
	 * information used in {@code equals} comparisons on the
	 * objects is modified.
	 * <li>For any non-null reference value {@code x},
	 * {@code x.equals(null)} should return {@code false}.
	 * </ul>
	 * <p>
	 * The {@code equals} method for class {@code Object} implements
	 * the most discriminating possible equivalence relation on objects;
	 * that is, for any non-null reference values {@code x} and
	 * {@code y}, this method returns {@code true} if and only
	 * if {@code x} and {@code y} refer to the same object
	 * ({@code x == y} has the value {@code true}).
	 * <p>
	 * Note that it is generally necessary to override the {@code hashCode}
	 * method whenever this method is overridden, so as to maintain the
	 * general contract for the {@code hashCode} method, which states
	 * that equal objects must have equal hash codes.
	 *
	 * @param obj the reference object with which to compare.
	 * @return {@code true} if this object is the same as the obj
	 * argument; {@code false} otherwise.
	 * @see #hashCode()
	 * @see HashMap
	 */
	@Override
	public boolean equals(Object obj)
	{
		if (!(obj instanceof BasicEvent))
		{
			return false;
		}
		return m_code == ((BasicEvent) obj).m_code;
	}

	/**
	 * Returns a hash code value for the object. This method is
	 * supported for the benefit of hash tables such as those provided by
	 * {@link HashMap}.
	 * <p>
	 * The general contract of {@code hashCode} is:
	 * <ul>
	 * <li>Whenever it is invoked on the same object more than once during
	 * an execution of a Java application, the {@code hashCode} method
	 * must consistently return the same integer, provided no information
	 * used in {@code equals} comparisons on the object is modified.
	 * This integer need not remain consistent from one execution of an
	 * application to another execution of the same application.
	 * <li>If two objects are equal according to the {@code equals(Object)}
	 * method, then calling the {@code hashCode} method on each of
	 * the two objects must produce the same integer result.
	 * <li>It is <em>not</em> required that if two objects are unequal
	 * according to the {@link Object#equals(Object)}
	 * method, then calling the {@code hashCode} method on each of the
	 * two objects must produce distinct integer results.  However, the
	 * programmer should be aware that producing distinct integer results
	 * for unequal objects may improve the performance of hash tables.
	 * </ul>
	 * <p>
	 * As much as is reasonably practical, the hashCode method defined by
	 * class {@code Object} does return distinct integers for distinct
	 * objects. (This is typically implemented by converting the internal
	 * address of the object into an integer, but this implementation
	 * technique is not required by the
	 * Java&trade; programming language.)
	 *
	 * @return a hash code value for this object.
	 * @see Object#equals(Object)
	 * @see System#identityHashCode
	 */
	@Override
	public int hashCode()
	{
		return getCode();
	}

	@Override
	public String toString()
	{
		return String.format("Event(%,d; %s)", getCode(), getName());
	}





	public static class LogMessage
	{
		private BasicEvent		m_event			= null;
		private String			m_message		= null;

		public LogMessage()
		{
			// For object pool
		}

		public LogMessage(final BasicEvent event, final String message)
		{
			set(event, message);
		}
		public void set(final BasicEvent event, final String message)
		{
			m_event = event;
			m_message = message;
		}

		// Called reflexively by BasicEventPatternLayout
		public String getEventCode()
		{
			final int		index			= m_event.getCode();
			return Integer.toString(index);
		}

		// Called reflexively by BasicEventPatternLayout
		public String getEventName()
		{
			final String description		= m_event.getName();
			return description;
		}

		@Override
		public String toString()
		{
			return m_message;
		}
	}
}
