async function apiRead(apiUrl, queries) {
  let url = apiUrl + '?';
  for (let key in queries) {
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
}

async function apiCreate(apiUrl, queries, obj) {
  let url = apiUrl + '?';
  for (let key in queries) {
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
}

async function apiUpdate(apiUrl, queries, obj) {
  let url = apiUrl + '?';
  for (let key in queries) {
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
}

async function apiDelete(apiUrl, queries) {
  let url = apiUrl + '?';
  for (let key in queries) {
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