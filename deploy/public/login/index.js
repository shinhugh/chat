// Required: /public/api.js

// --------------------------------------------------

// Constants

const userApi = '/api/user';
const loginApi = '/api/login';

// --------------------------------------------------

// DOM elements

const loginTab = document.getElementById('button_login_tab');
const signupTab = document.getElementById('button_signup_tab');
const loginFields = document.getElementById('div_login_fields');
const loginUserName = document.getElementById('input_login_user_name');
const loginUserPw = document.getElementById('input_login_user_pw');
const loginSubmit = document.getElementById('button_login_submit');
const signupFields = document.getElementById('div_signup_fields');
const signupUserName = document.getElementById('input_signup_user_name');
const signupUserPw = document.getElementById('input_signup_user_pw');
const signupSubmit = document.getElementById('button_signup_submit');
const overlayNotification = document.getElementById('p_overlay_notification');

// --------------------------------------------------

// Switch between tabs

loginTab.onclick = () => {
  loginTab.disabled = true;
  if (!loginFields.hidden) {
    return;
  }
  signupTab.disabled = true;
  signupFields.hidden = true;
  loginFields.hidden = false;
  signupTab.disabled = false;
  loginUserName.focus();
};

signupTab.onclick = () => {
  signupTab.disabled = true;
  if (!signupFields.hidden) {
    return;
  }
  loginTab.disabled = true;
  loginFields.hidden = true;
  signupFields.hidden = false;
  loginTab.disabled = false;
  signupUserName.focus();
};

// --------------------------------------------------

// Login

loginSubmit.onclick = () => {
  loginSubmit.disabled = true;
  if (loginUserName.value == '' || loginUserPw.value == '') {
    loginSubmit.disabled = false;
    return;
  }
  let userName = loginUserName.value;
  let userPw = loginUserPw.value;
  apiCreate(loginApi, null, {
    'name': userName,
    'pw': userPw
  })
  .then(() => {
    location.href = '/';
  })
  .catch(() => {
    showOverlayNotification('Unable to login', 2000);
    loginSubmit.disabled = false;
  });
};

loginUserName.addEventListener('keypress', (event) => {
  if (event.key == 'Enter') {
    event.preventDefault();
    loginUserPw.focus();
  }
});

loginUserPw.addEventListener('keypress', (event) => {
  if (event.key == 'Enter') {
    event.preventDefault();
    loginSubmit.click();
  }
});

loginUserName.focus();

// --------------------------------------------------

// Sign up

signupSubmit.onclick = () => {
  signupSubmit.disabled = true;
  if (signupUserName.value == '' || signupUserPw.value == '') {
    signupSubmit.disabled = false;
    return;
  }
  let userName = signupUserName.value;
  let userPw = signupUserPw.value;
  apiCreate(userApi, null, {
    'name': userName,
    'pw': userPw
  })
  .then(() => {
    return apiCreate(loginApi, null, {
      'name': userName,
      'pw': userPw
    });
  })
  .then(() => {
    location.href = '/';
  })
  .catch(() => {
    showOverlayNotification('Unable to sign up or login', 2000);
    signupSubmit.disabled = false;
  });
};

signupUserName.addEventListener('keypress', (event) => {
  if (event.key == 'Enter') {
    event.preventDefault();
    signupUserPw.focus();
  }
});

signupUserPw.addEventListener('keypress', (event) => {
  if (event.key == 'Enter') {
    event.preventDefault();
    signupSubmit.click();
  }
});

// --------------------------------------------------

// Display an overlay notification

var notificationTimeout;

function showOverlayNotification(message, timeout) {
  clearTimeout(notificationTimeout);
  overlayNotification.innerHTML = message;
  overlayNotification.hidden = false;
  notificationTimeout = setTimeout(() => {
    overlayNotification.hidden = true;
    overlayNotification.innerHTML = '';
  }, timeout);
}