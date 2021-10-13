import axios from 'axios'

const AXIOS = axios.create({
  baseURL: `/api`,
  //timeout: 0,
});


export default {
    hello() {
        return AXIOS.get(`/hello`);
    },
    showGDSRecord() {
        return AXIOS.get(`/showGDSRecord`);
    },
    getUser(userId) {
        return AXIOS.get(`/user/` + userId);
    },
    createUser(firstName, lastName) {
        return AXIOS.post(`/user/` + firstName + '/' + lastName);
    },
    getSecured(user, password) {
        return AXIOS.get(`/secured/`,{
            auth: {
                username: user,
                password: password
            }});
    },
    processKeyword(keyword) {
        console.log('processKeyword keyword: '+keyword)
        console.log(`?x=${encodeURIComponent(keyword)}`);
        return AXIOS.post(`/processKeyword/`+encodeURIComponent(keyword));
    },
    getGeoOmnibusGDS(keyword) {
        console.log('getGeoOmnibusGDS keyword: '+keyword)
        console.log(`?x=${encodeURIComponent(keyword)}`);
        return AXIOS.post(`/getGeoOmnibusGDS/`+encodeURIComponent(keyword));
    },
    populateDBWithGDS() {
        console.log('populateDBWithGDS');
        return AXIOS.get(`/populateDBWithGDS`);
    },
    getNumValidatedRecords() {
        console.log('getNumValidatedRecords');
        return AXIOS.get(`/getNumValidatedRecords`);
    },
    getInvalidRecords() {
        console.log('getInvalidRecords');
        return AXIOS.get(`/getInvalidRecords`);
    },
    setInvalidRecordsAsValid(items) {
        console.log('setInvalidRecordsAsValid');
        return AXIOS.post('/setInvalidRecordsAsValid', items, {
            headers: {
              // Overwrite Axios's automatically set Content-Type
              'Content-Type': 'application/json'
            }
        });
    },
    setValidRecordsAsInvalid(items) {
        console.log('setValidRecordsAsInvalid');
        return AXIOS.post('/setValidRecordsAsInvalid', items, {
            headers: {
              // Overwrite Axios's automatically set Content-Type
              'Content-Type': 'application/json'
            }
        });
    },
    transferRulelist(params) {
        console.log('transferRulelist params='+params);
        return AXIOS.post(`/transferRulelist`, params, {
            headers: {
              // Overwrite Axios's automatically set Content-Type
              'Content-Type': 'application/json'
            }
        });
    },
    getNewTabRecords(params) {
        console.log('getNewTabRecords');
        return AXIOS.post(`/getNewTabRecords`, params, {
            headers: {
              // Overwrite Axios's automatically set Content-Type
              'Content-Type': 'application/json'
            }
        });
    },
    async downloadSOFT(items, foldername) {
        console.log('downloadSOFT');
        return AXIOS.post('/downloadSOFT', items, {
            headers: {
              // Overwrite Axios's automatically set Content-Type
              'Content-Type': 'application/json'
            }
        });
    },
    setDownloadFoldername(foldername) {
        console.log('setDownloadFoldername');
        return AXIOS.post('/setDownloadFoldername', foldername);
    },
    setupPython() {
        console.log('setupPython');
        return AXIOS.post('/setupPython');
    },
}


