const apiUtilities = {
  'verifyFields': (obj, fields) => {
    try {
      for (const field of fields) {
        if (!obj.hasOwnProperty(field)) {
          return false;
        }
      }
      return true;
    }
    catch (error) {
      return false;
    }
  }
};
