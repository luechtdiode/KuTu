(window.webpackJsonp=window.webpackJsonp||[]).push([[17],{DCTv:function(l,n,t){"use strict";t.r(n);var e=t("CcnG"),u=t("ZZ/e"),o=t("cygB"),i=t("VnD/"),r=function(){function l(l,n){this.navCtrl=l,this.backendService=n,this.items=[],this.backendService.getCompetitions()}return l.prototype.ngOnInit=function(){var l=this;this.backendService.loadAlleResultate().subscribe(function(n){l.geraete=n,l.sortItems()}),this.backendService.newLastResults.pipe(Object(i.a)(function(l){return!!l&&!!l.results})).subscribe(function(n){l.lastItems=l.items.map(function(n){return n.id*l.geraete.length+n.geraet}),l.items=[],Object.keys(n.results).forEach(function(t){l.items.push(n.results[t])}),l.sortItems()})},l.prototype.sortItems=function(){var l=this;this.items=this.items.sort(function(n,t){var e=n.programm.localeCompare(t.programm);return 0===e&&(e=l.geraetOrder(n.geraet)-l.geraetOrder(t.geraet)),e}).filter(function(l){return void 0!==l.wertung.endnote})},l.prototype.isNew=function(l){var n=this;return 0===this.lastItems.filter(function(t){return t===l.id*n.geraete.length+l.geraet}).length},Object.defineProperty(l.prototype,"stationFreezed",{get:function(){return this.backendService.stationFreezed},enumerable:!0,configurable:!0}),Object.defineProperty(l.prototype,"competition",{get:function(){return this.backendService.competition||""},set:function(l){var n=this;this.stationFreezed||(this.backendService.getDurchgaenge(l),this.backendService.loadAlleResultate().subscribe(function(l){n.geraete=l,n.sortItems()}))},enumerable:!0,configurable:!0}),l.prototype.getCompetitions=function(){return this.backendService.competitions||[]},l.prototype.competitionName=function(){var l=this;if(!this.backendService.competitions)return"";var n=this.backendService.competitions.filter(function(n){return n.uuid===l.backendService.competition}).map(function(l){return l.titel+", am "+(l.datum+"T").split("T")[0].split("-").reverse().join("-")});return 1===n.length?n[0]:""},l.prototype.geraetOrder=function(l){return this.geraete?this.geraete.findIndex(function(n){return n.id===l}):0},l.prototype.geraetText=function(l){if(!this.geraete)return"";var n=this.geraete.filter(function(n){return n.id===l}).map(function(l){return l.name});return 1===n.length?n[0]:""},l.prototype.getTitle=function(l){return l.programm+" - "+this.geraetText(l.geraet)},l.prototype.itemTapped=function(l,n){},l}(),b=function(){return function(){}}(),c=t("pMnS"),a=t("oBZk"),s=t("gIcY"),p=t("Ip0R"),g=t("wj5n"),d=t("H2Ii"),m=e.nb({encapsulation:0,styles:[[""]],data:{}});function h(l){return e.Hb(0,[(l()(),e.pb(0,0,null,null,3,"ion-select-option",[],null,null,null,a.fb,a.B)),e.ob(1,49152,null,0,u.nb,[e.h,e.k],{value:[0,"value"]},null),(l()(),e.Fb(2,0,[" ",""])),e.Bb(3,2)],function(l,n){l(n,1,0,n.context.$implicit.uuid)},function(l,n){var t=n.context.$implicit.titel+" "+e.Gb(n,2,0,l(n,3,0,e.yb(n.parent.parent.parent,0),n.context.$implicit.datum,"dd-MM-yy"));l(n,2,0,t)})}function f(l){return e.Hb(0,[(l()(),e.pb(0,0,null,null,8,"ion-select",[["cancelText","Abbrechen"],["okText","Okay"],["placeholder","Bitte ausw\xe4hlen"]],[[2,"ng-untouched",null],[2,"ng-touched",null],[2,"ng-pristine",null],[2,"ng-dirty",null],[2,"ng-valid",null],[2,"ng-invalid",null],[2,"ng-pending",null]],[[null,"ngModelChange"],[null,"ionBlur"],[null,"ionChange"]],function(l,n,t){var u=!0,o=l.component;return"ionBlur"===n&&(u=!1!==e.yb(l,1)._handleBlurEvent()&&u),"ionChange"===n&&(u=!1!==e.yb(l,1)._handleChangeEvent(t.target.value)&&u),"ngModelChange"===n&&(u=!1!==(o.competition=t)&&u),u},a.gb,a.A)),e.ob(1,16384,null,0,u.Lb,[e.k],null,null),e.Cb(1024,null,s.d,function(l){return[l]},[u.Lb]),e.ob(3,671744,null,0,s.i,[[8,null],[8,null],[8,null],[6,s.d]],{model:[0,"model"]},{update:"ngModelChange"}),e.Cb(2048,null,s.e,null,[s.i]),e.ob(5,16384,null,0,s.f,[[4,s.e]],null,null),e.ob(6,49152,null,0,u.mb,[e.h,e.k],{cancelText:[0,"cancelText"],okText:[1,"okText"],placeholder:[2,"placeholder"]},null),(l()(),e.gb(16777216,null,0,1,null,h)),e.ob(8,278528,null,0,p.k,[e.O,e.L,e.s],{ngForOf:[0,"ngForOf"]},null)],function(l,n){var t=n.component;l(n,3,0,t.competition),l(n,6,0,"Abbrechen","Okay","Bitte ausw\xe4hlen"),l(n,8,0,t.getCompetitions())},function(l,n){l(n,0,0,e.yb(n,5).ngClassUntouched,e.yb(n,5).ngClassTouched,e.yb(n,5).ngClassPristine,e.yb(n,5).ngClassDirty,e.yb(n,5).ngClassValid,e.yb(n,5).ngClassInvalid,e.yb(n,5).ngClassPending)})}function k(l){return e.Hb(0,[(l()(),e.pb(0,0,null,null,8,"ion-col",[],null,null,null,a.L,a.g)),e.ob(1,49152,null,0,u.t,[e.h,e.k],null,null),(l()(),e.pb(2,0,null,0,6,"ion-item",[],null,null,null,a.W,a.n)),e.ob(3,49152,null,0,u.H,[e.h,e.k],null,null),(l()(),e.pb(4,0,null,0,2,"ion-label",[],null,null,null,a.X,a.s)),e.ob(5,49152,null,0,u.N,[e.h,e.k],null,null),(l()(),e.Fb(-1,0,["Wettkampf"])),(l()(),e.gb(16777216,null,0,1,null,f)),e.ob(8,16384,null,0,p.l,[e.O,e.L],{ngIf:[0,"ngIf"]},null)],function(l,n){l(n,8,0,n.component.getCompetitions().length>0)},null)}function v(l){return e.Hb(0,[(l()(),e.pb(0,0,null,null,2,"ion-note",[["slot","end"]],null,null,null,a.bb,a.w)),e.ob(1,49152,null,0,u.X,[e.h,e.k],null,null),(l()(),e.Fb(2,0,[" "," "]))],null,function(l,n){l(n,2,0,n.component.competitionName())})}function y(l){return e.Hb(0,[(l()(),e.pb(0,0,null,null,6,"ion-col",[["class","align-self-start"],["col-12",""],["col-lg-4",""],["col-sm-6",""],["col-xl-2",""],["size-lg","4"],["size-md","6"],["size-xl","2"],["size-xs","12"]],null,[[null,"click"]],function(l,n,t){var e=!0;return"click"===n&&(e=!1!==l.component.itemTapped(t,l.context.$implicit)&&e),e},a.L,a.g)),e.ob(1,49152,null,0,u.t,[e.h,e.k],null,null),(l()(),e.pb(2,0,null,0,4,"div",[],null,null,null,null,null)),e.ob(3,278528,null,0,p.j,[e.s,e.t,e.k,e.D],{ngClass:[0,"ngClass"]},null),e.Ab(4,{"gradient-border":0}),(l()(),e.pb(5,0,null,null,1,"result-display",[],null,null,null,g.b,g.a)),e.ob(6,114688,null,0,d.a,[],{item:[0,"item"],title:[1,"title"]},null)],function(l,n){var t=n.component,e=l(n,4,0,t.isNew(n.context.$implicit));l(n,3,0,e),l(n,6,0,n.context.$implicit,t.getTitle(n.context.$implicit))},null)}function w(l){return e.Hb(0,[e.zb(0,p.d,[e.u]),(l()(),e.pb(1,0,null,null,26,"ion-header",[],null,null,null,a.P,a.k)),e.ob(2,49152,null,0,u.B,[e.h,e.k],null,null),(l()(),e.pb(3,0,null,0,24,"ion-toolbar",[],null,null,null,a.jb,a.E)),e.ob(4,49152,null,0,u.Bb,[e.h,e.k],null,null),(l()(),e.pb(5,0,null,0,7,"ion-buttons",[["slot","start"]],null,null,null,a.J,a.e)),e.ob(6,49152,null,0,u.l,[e.h,e.k],null,null),(l()(),e.pb(7,0,null,0,5,"ion-menu-toggle",[],null,null,null,a.Z,a.v)),e.ob(8,49152,null,0,u.S,[e.h,e.k],null,null),(l()(),e.pb(9,0,null,0,3,"ion-button",[],null,null,null,a.I,a.d)),e.ob(10,49152,null,0,u.k,[e.h,e.k],null,null),(l()(),e.pb(11,0,null,0,1,"ion-icon",[["name","menu"],["slot","icon-only"]],null,null,null,a.Q,a.l)),e.ob(12,49152,null,0,u.C,[e.h,e.k],{name:[0,"name"]},null),(l()(),e.pb(13,0,null,0,12,"ion-title",[],null,null,null,a.ib,a.D)),e.ob(14,49152,null,0,u.zb,[e.h,e.k],null,null),(l()(),e.pb(15,0,null,0,10,"ion-grid",[["no-padding",""]],null,null,null,a.O,a.j)),e.ob(16,49152,null,0,u.A,[e.h,e.k],null,null),(l()(),e.pb(17,0,null,0,8,"ion-row",[],null,null,null,a.db,a.y)),e.ob(18,49152,null,0,u.ib,[e.h,e.k],null,null),(l()(),e.pb(19,0,null,0,4,"ion-col",[],null,null,null,a.L,a.g)),e.ob(20,49152,null,0,u.t,[e.h,e.k],null,null),(l()(),e.pb(21,0,null,0,2,"ion-label",[],null,null,null,a.X,a.s)),e.ob(22,49152,null,0,u.N,[e.h,e.k],null,null),(l()(),e.Fb(-1,0,["Aktuelle Resultate"])),(l()(),e.gb(16777216,null,0,1,null,k)),e.ob(25,16384,null,0,p.l,[e.O,e.L],{ngIf:[0,"ngIf"]},null),(l()(),e.gb(16777216,null,0,1,null,v)),e.ob(27,16384,null,0,p.l,[e.O,e.L],{ngIf:[0,"ngIf"]},null),(l()(),e.pb(28,0,null,null,7,"ion-content",[],null,null,null,a.M,a.h)),e.ob(29,49152,null,0,u.u,[e.h,e.k],null,null),(l()(),e.pb(30,0,null,0,5,"ion-grid",[["no-padding",""]],null,null,null,a.O,a.j)),e.ob(31,49152,null,0,u.A,[e.h,e.k],null,null),(l()(),e.pb(32,0,null,0,3,"ion-row",[],null,null,null,a.db,a.y)),e.ob(33,49152,null,0,u.ib,[e.h,e.k],null,null),(l()(),e.gb(16777216,null,0,1,null,y)),e.ob(35,278528,null,0,p.k,[e.O,e.L,e.s],{ngForOf:[0,"ngForOf"]},null)],function(l,n){var t=n.component;l(n,12,0,"menu"),l(n,25,0,!t.competition),l(n,27,0,t.competition),l(n,35,0,t.items)},null)}function C(l){return e.Hb(0,[(l()(),e.pb(0,0,null,null,1,"app-last-results",[],null,null,null,w,m)),e.ob(1,114688,null,0,r,[u.Hb,o.a],null,null)],function(l,n){l(n,1,0)},null)}var O=e.lb("app-last-results",r,C,{},{},[]),x=t("ZYCi"),I=t("bDjW");t.d(n,"LastResultsPageModuleNgFactory",function(){return T});var T=e.mb(b,[],function(l){return e.vb([e.wb(512,e.j,e.bb,[[8,[c.a,O]],[3,e.j],e.x]),e.wb(4608,p.n,p.m,[e.u,[2,p.t]]),e.wb(4608,s.m,s.m,[]),e.wb(4608,u.b,u.b,[e.z,e.g]),e.wb(4608,u.Gb,u.Gb,[u.b,e.j,e.q,p.c]),e.wb(4608,u.Kb,u.Kb,[u.b,e.j,e.q,p.c]),e.wb(1073742336,p.b,p.b,[]),e.wb(1073742336,s.k,s.k,[]),e.wb(1073742336,s.b,s.b,[]),e.wb(1073742336,u.Db,u.Db,[]),e.wb(1073742336,x.n,x.n,[[2,x.t],[2,x.m]]),e.wb(1073742336,I.a,I.a,[]),e.wb(1073742336,b,b,[]),e.wb(1024,x.k,function(){return[[{path:"",component:r}]]},[])])})}}]);