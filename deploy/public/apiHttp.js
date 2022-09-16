const apiHttp = {
  'read': async (url, queries) => {
    url = url + '?';
    for (const key in queries) {
      url += key + '=' + queries[key] + '&';
    }
    url = url.slice(0, -1);
    const response = await fetch(url, {
      'credentials': 'same-origin'
    });
    if (response.status != 200) {
      throw 'Request unsuccessful';
    }
    try {
      return response.json();
    } catch {
      throw 'Response body was not in JSON format';
    }
  },

  'create': async (url, queries, obj) => {
    url = url + '?';
    for (const key in queries) {
      url += key + '=' + queries[key] + '&';
    }
    url = url.slice(0, -1);
    const response = await fetch(url, {
      'method': 'POST',
      'credentials': 'same-origin',
      'headers': {
        'Content-Type': 'application/json'
      },
      'body': JSON.stringify(obj)
    });
    if (response.status != 200) {
      throw 'Request unsuccessful';
    }
  },

  'update': async (url, queries, obj) => {
    url = url + '?';
    for (const key in queries) {
      url += key + '=' + queries[key] + '&';
    }
    url = url.slice(0, -1);
    const response = await fetch(url, {
      'method': 'PUT',
      'credentials': 'same-origin',
      'headers': {
        'Content-Type': 'application/json'
      },
      'body': JSON.stringify(obj)
    });
    if (response.status != 200) {
      throw 'Request unsuccessful';
    }
  },

  'delete': async (url, queries) => {
    url = url + '?';
    for (const key in queries) {
      url += key + '=' + queries[key] + '&';
    }
    url = url.slice(0, -1);
    const response = await fetch(url, {
      'method': 'DELETE',
      'credentials': 'same-origin'
    });
    if (response.status != 200) {
      throw 'Request unsuccessful';
    }
  }
};
