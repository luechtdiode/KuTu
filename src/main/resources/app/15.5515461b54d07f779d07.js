(window.webpackJsonp=window.webpackJsonp||[]).push([[15],{gNol:function(n,l,t){"use strict";t.r(l);var e=t("CcnG"),u=t("ZZ/e"),i=t("cygB"),r=t("VnD/"),o=t("67Y/"),b=function(){function n(n,l,t){this.navCtrl=n,this.route=l,this.backendService=t,this.items=[],this.lastItems=[]}return Object.defineProperty(n.prototype,"geraete",{get:function(){return this.backendService.geraete},enumerable:!0,configurable:!0}),n.prototype.ngOnInit=function(){var n=this,l=parseInt(this.route.snapshot.paramMap.get("athletId")),t=this.route.snapshot.paramMap.get("wkId");this.backendService.loadAthletWertungen(t,l).pipe(Object(r.a)(function(n){return!!n&&n.length>0})).subscribe(function(t){n.items=t,n.sortItems(l)}),this.backendService.newLastResults.pipe(Object(r.a)(function(n){return!!n}),Object(o.a)(function(n){return n.results})).subscribe(function(l){n.lastItems=n.items.map(function(l){return l.id*n.geraete.length+l.geraet}),n.items=n.items.map(function(n){var t=l[n.wertung.wettkampfdisziplinId];return t&&t.id===n.id?t:n})})},n.prototype.sortItems=function(n){var l=this;this.items=this.items.sort(function(n,t){var e=n.programm.localeCompare(t.programm);return 0===e&&(e=l.geraetOrder(n.geraet)-l.geraetOrder(t.geraet)),e})},n.prototype.isNew=function(n){var l=this;return 0===this.lastItems.filter(function(t){return t===n.id*l.geraete.length+n.geraet}).length},Object.defineProperty(n.prototype,"stationFreezed",{get:function(){return this.backendService.stationFreezed},enumerable:!0,configurable:!0}),Object.defineProperty(n.prototype,"competition",{get:function(){return this.backendService.competition||""},enumerable:!0,configurable:!0}),n.prototype.getCompetitions=function(){return this.backendService.competitions||[]},n.prototype.competitionName=function(){var n=this;if(!this.backendService.competitions)return"";var l=this.backendService.competitions.filter(function(l){return l.uuid===n.backendService.competition}).map(function(n){return n.titel+", am "+(n.datum+"T").split("T")[0].split("-").reverse().join("-")});return 1===l.length?l[0]:""},n.prototype.geraetOrder=function(n){return this.geraete?this.geraete.findIndex(function(l){return l.id===n}):0},n.prototype.geraetText=function(n){if(!this.geraete)return"";var l=this.geraete.filter(function(l){return l.id===n}).map(function(n){return n.name});return 1===l.length?l[0]:""},n.prototype.getTitle=function(n){return n.programm+" - "+this.geraetText(n.geraet)},n}(),a=function(){return function(){}}(),c=t("pMnS"),s=t("oBZk"),p=t("Ip0R"),f=t("wj5n"),g=t("H2Ii"),m=t("ZYCi"),h=e.nb({encapsulation:0,styles:[[""]],data:{}});function d(n){return e.Hb(0,[(n()(),e.pb(0,0,null,null,2,"ion-note",[["slot","end"]],null,null,null,s.bb,s.w)),e.ob(1,49152,null,0,u.W,[e.h,e.k],null,null),(n()(),e.Fb(2,0,[" "," "]))],null,function(n,l){n(l,2,0,l.component.competitionName())})}function k(n){return e.Hb(0,[(n()(),e.pb(0,0,null,null,6,"ion-col",[["class","align-self-start"],["col-12",""],["col-lg-4",""],["col-sm-6",""],["col-xl-2",""],["size-lg","4"],["size-md","6"],["size-xl","2"],["size-xs","12"]],null,null,null,s.L,s.g)),e.ob(1,49152,null,0,u.s,[e.h,e.k],null,null),(n()(),e.pb(2,0,null,0,4,"div",[],null,null,null,null,null)),e.ob(3,278528,null,0,p.j,[e.s,e.t,e.k,e.D],{ngClass:[0,"ngClass"]},null),e.Ab(4,{"gradient-border":0}),(n()(),e.pb(5,0,null,null,1,"result-display",[],null,null,null,f.b,f.a)),e.ob(6,114688,null,0,g.a,[],{item:[0,"item"],title:[1,"title"]},null)],function(n,l){var t=l.component,e=n(l,4,0,t.isNew(l.context.$implicit));n(l,3,0,e),n(l,6,0,l.context.$implicit,t.getTitle(l.context.$implicit))},null)}function w(n){return e.Hb(0,[(n()(),e.pb(0,0,null,null,21,"ion-header",[],null,null,null,s.P,s.k)),e.ob(1,49152,null,0,u.A,[e.h,e.k],null,null),(n()(),e.pb(2,0,null,0,19,"ion-toolbar",[],null,null,null,s.jb,s.E)),e.ob(3,49152,null,0,u.Ab,[e.h,e.k],null,null),(n()(),e.pb(4,0,null,0,4,"ion-buttons",[["slot","start"]],null,null,null,s.J,s.e)),e.ob(5,49152,null,0,u.k,[e.h,e.k],null,null),(n()(),e.pb(6,0,null,0,2,"ion-back-button",[["defaultHref","/"]],null,[[null,"click"]],function(n,l,t){var u=!0;return"click"===l&&(u=!1!==e.yb(n,8).onClick(t)&&u),u},s.H,s.c)),e.ob(7,49152,null,0,u.f,[e.h,e.k],{defaultHref:[0,"defaultHref"]},null),e.ob(8,16384,null,0,u.g,[[2,u.gb],u.Gb],{defaultHref:[0,"defaultHref"]},null),(n()(),e.pb(9,0,null,0,10,"ion-title",[],null,null,null,s.ib,s.D)),e.ob(10,49152,null,0,u.yb,[e.h,e.k],null,null),(n()(),e.pb(11,0,null,0,8,"ion-grid",[["no-padding",""]],null,null,null,s.O,s.j)),e.ob(12,49152,null,0,u.z,[e.h,e.k],null,null),(n()(),e.pb(13,0,null,0,6,"ion-row",[],null,null,null,s.db,s.y)),e.ob(14,49152,null,0,u.hb,[e.h,e.k],null,null),(n()(),e.pb(15,0,null,0,4,"ion-col",[],null,null,null,s.L,s.g)),e.ob(16,49152,null,0,u.s,[e.h,e.k],null,null),(n()(),e.pb(17,0,null,0,2,"ion-label",[],null,null,null,s.X,s.s)),e.ob(18,49152,null,0,u.M,[e.h,e.k],null,null),(n()(),e.Fb(-1,0,["Aktuelle Resultate"])),(n()(),e.gb(16777216,null,0,1,null,d)),e.ob(21,16384,null,0,p.l,[e.O,e.L],{ngIf:[0,"ngIf"]},null),(n()(),e.pb(22,0,null,null,7,"ion-content",[],null,null,null,s.M,s.h)),e.ob(23,49152,null,0,u.t,[e.h,e.k],null,null),(n()(),e.pb(24,0,null,0,5,"ion-grid",[["no-padding",""]],null,null,null,s.O,s.j)),e.ob(25,49152,null,0,u.z,[e.h,e.k],null,null),(n()(),e.pb(26,0,null,0,3,"ion-row",[],null,null,null,s.db,s.y)),e.ob(27,49152,null,0,u.hb,[e.h,e.k],null,null),(n()(),e.gb(16777216,null,0,1,null,k)),e.ob(29,278528,null,0,p.k,[e.O,e.L,e.s],{ngForOf:[0,"ngForOf"]},null)],function(n,l){var t=l.component;n(l,7,0,"/"),n(l,8,0,"/"),n(l,21,0,t.competition),n(l,29,0,t.items)},null)}function v(n){return e.Hb(0,[(n()(),e.pb(0,0,null,null,1,"app-athlet-view",[],null,null,null,w,h)),e.ob(1,114688,null,0,b,[u.Gb,m.a,i.a],null,null)],function(n,l){n(l,1,0)},null)}var y=e.lb("app-athlet-view",b,v,{},{},[]),j=t("gIcY"),I=t("bDjW");t.d(l,"AthletViewPageModuleNgFactory",function(){return O});var O=e.mb(a,[],function(n){return e.vb([e.wb(512,e.j,e.bb,[[8,[c.a,y]],[3,e.j],e.x]),e.wb(4608,p.n,p.m,[e.u,[2,p.t]]),e.wb(4608,j.m,j.m,[]),e.wb(4608,u.b,u.b,[e.z,e.g]),e.wb(4608,u.Fb,u.Fb,[u.b,e.j,e.q,p.c]),e.wb(4608,u.Jb,u.Jb,[u.b,e.j,e.q,p.c]),e.wb(1073742336,p.b,p.b,[]),e.wb(1073742336,j.k,j.k,[]),e.wb(1073742336,j.b,j.b,[]),e.wb(1073742336,u.Cb,u.Cb,[]),e.wb(1073742336,m.n,m.n,[[2,m.t],[2,m.m]]),e.wb(1073742336,I.a,I.a,[]),e.wb(1073742336,a,a,[]),e.wb(1024,m.k,function(){return[[{path:"",component:b}]]},[])])})}}]);