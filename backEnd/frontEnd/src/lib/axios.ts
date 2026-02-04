import axios from "axios";
// export const api = axios.create({ baseURL: import.meta.env.VITE_API_BASE?? "https://erpproject-pu8e.onrender.com",withCredentials: false, });
export const api = axios.create({ baseURL: import.meta.env.VITE_API_BASE?? "http://localhost:8080",withCredentials: false, });
