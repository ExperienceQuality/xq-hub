function fn() {
  // Framework defaults for the XQ Karate POC.
  // Bound via Java.type here so consumer features never call Java.type.
  var Support = Java.type('com.xq.karate.XqKarateSupport');
  var baseUrl = karate.properties['xq.baseUrl'] || 'https://httpbin.org';
  var boot = Support.bootstrap(baseUrl);
  return {
    baseUrl: boot.baseUrl,
    xqRequestId: boot.xqRequestId,
    xqEcho: boot.xqEcho,
    xqFramework: 'xq-karate-support'
  };
}
