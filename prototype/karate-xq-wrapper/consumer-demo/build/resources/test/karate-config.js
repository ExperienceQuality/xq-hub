function fn() {
  // Consumer-owned thin config. Framework defaults come from karate-base.js
  // shipped by xq-karate-support (if discovered on the classpath).
  var framework = karate.get('config') || {};
  var config = karate.merge(framework, {
    projectName: 'consumer-demo',
    consumerNote: 'thin project config on top of xq base'
  });
  return config;
}
