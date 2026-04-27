const dns = require('dns');
dns.setServers(['8.8.8.8', '8.8.4.4']);
dns.resolve4('github.com', (err, addresses) => {
  if (err) {
    console.error('resolve4 err:', err.message);
  } else {
    console.log('resolve4 success:', addresses);
  }
});
