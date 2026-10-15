import axios from "axios";
import Element from 'element-ui'
import router from "../router/index";
axios.defaults.baseURL = "http://localhost:8888"
const request = axios.create({
    timeout: 5000,
    headers: {
        'Content-Type': 'application/json; charset=utf-8'
    }
})
request.interceptors.request.use(config => {

    config.headers['Authorization'] = localStorage.getItem("token") // 请求头带上token
    return config
})
request.interceptors.response.use(response => {
        let res = response.data;


        if (res.code === 200) {
            return response.data
        } else {
            Element.Message.error(res.msg? res.msg : '系统异常！', {duration: 3 * 1000})
            return Promise.reject(response.data.msg)
        }
    },
    error => {
        if(error.response.status === 401) {
            localStorage.clear()
            router.push("/login")
        }
        if(error.response.data) {
            error.message = error.response.data.msg
        }

        Element.Message.error(error.message, {duration: 3 * 1000})
        return Promise.reject(error)
    }
)
export default request
