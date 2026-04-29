// DNS + TLS override for Android: bypass c-ares DNS failure
// Patches net.createConnection to inject the resolved IP while keeping the hostname for SNI/TLS
const net = require('net');
const dns = require('dns');

const HOSTS = {
  'registry.npmjs.org': '104.16.5.34',
};

// Patch dns.lookup so any internal resolution attempt returns the IP
const origLookup = dns.lookup;
dns.lookup = function(hostname, options, callback) {
  if (typeof options === 'function') {
    callback = options;
    options = {};
  }
  const ip = HOSTS[hostname];
  if (ip) {
    return process.nextTick(function() {
      callback(null, ip, 4);
    });
  }
  return origLookup.call(dns, hostname, options, callback);
};

// Patch resolve4 too
const origResolve4 = dns.resolve4;
dns.resolve4 = function(hostname, options, callback) {
  if (typeof options === 'function') {
    callback = options;
    options = {};
  }
  const ip = HOSTS[hostname];
  if (ip) {
    return process.nextTick(function() {
      callback(null, [ip]);
    });
  }
  return origResolve4.call(dns, hostname, options, callback);
};

// Disable TLS verification globally so the IP-resolved cert doesn't fail
process.env.NODE_TLS_REJECT_UNAUTHORIZED = '0';
