import User from '../pages/User.vue'
import Index from '../pages/Index.vue'
import Team from '../pages/Team.vue'
import Search from '../pages/Search.vue'
import Edit from '../pages/UserEditPage.vue'



const routes = [
    { path: '/user/edit', component: Edit },
    { path: '/user', component: User },
    { path: '/', component: Index },
    { path: '/team', component: Team },
    { path: '/search', component: Search },
]

export default routes;

