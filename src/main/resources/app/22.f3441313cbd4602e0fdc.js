(window.webpackJsonp=window.webpackJsonp||[]).push([[22],{UK8Y:function(e,t,n){"use strict";n.r(t),n.d(t,"WertungEditorPageModule",(function(){return w}));var i=n("ofXK"),r=n("3Pt+"),o=n("tyNb"),s=n("TEn/"),a=n("mrSG"),c=n("cygB"),u=n("PLH8"),d=n("fXoL"),l=n("jhN1"),b=["wertungsform"],g=["enote"],h=["dnote"];function p(e,t){if(1&e&&(d.Kb(0,"ion-button",16,17),d.Ib(2,"ion-icon",18),d.kc(3,"Speichern & Weiter"),d.Jb()),2&e){var n=d.Ub(),i=d.dc(13);d.ac("disabled",n.waiting||!i.valid)}}function f(e,t){if(1&e){var n=d.Lb();d.Kb(0,"ion-button",19),d.Sb("click",(function(){d.ec(n);var e=d.Ub(),t=d.dc(13);return e.saveClose(t)})),d.Ib(1,"ion-icon",20),d.kc(2,"Speichern"),d.Jb()}if(2&e){var i=d.Ub(),r=d.dc(13);d.ac("disabled",i.waiting||!r.valid)}}var v=[{path:"",component:function(){function e(e,t,n,i,r,o,s,a,c){this.navCtrl=e,this.route=t,this.sanitizer=n,this.alertCtrl=i,this.toastController=r,this.keyboard=o,this.backendService=s,this.platform=a,this.zone=c,this.waiting=!1,this.isDNoteUsed=!0,this.durchgang=s.durchgang,this.step=s.step,this.geraetId=s.geraet;var u=parseInt(this.route.snapshot.paramMap.get("itemId"));this.updateUI(s.wertungen.find((function(e){return e.id===u}))),this.isDNoteUsed=this.item.isDNoteUsed}return e.prototype.ionViewWillLeave=function(){this.subscription&&(this.subscription.unsubscribe(),this.subscription=void 0)},e.prototype.ionViewWillEnter=function(){var e=this;this.subscription&&(this.subscription.unsubscribe(),this.subscription=void 0),this.subscription=this.backendService.wertungUpdated.subscribe((function(t){console.log("incoming wertung from service",t),t.wertung.athletId===e.wertung.athletId&&t.wertung.wettkampfdisziplinId===e.wertung.wettkampfdisziplinId&&t.wertung.endnote!==e.wertung.endnote&&(console.log("updateing wertung from service"),e.item.wertung=Object.assign({},t.wertung),e.itemOriginal.wertung=Object.assign({},t.wertung),e.wertung=Object.assign({noteD:0,noteE:0,endnote:0},e.item.wertung))})),this.platform.ready().then((function(){setTimeout((function(){e.keyboard.show&&(e.keyboard.show(),console.log("keyboard called")),e.isDNoteUsed&&e.dnote?(e.dnote.setFocus(),console.log("dnote focused")):e.enote&&(e.enote.setFocus(),console.log("enote focused"))}),400)}))},e.prototype.editable=function(){return this.backendService.loggedIn},e.prototype.updateUI=function(e){var t=this;this.zone.run((function(){t.waiting=!1,t.item=Object.assign({},e),t.itemOriginal=Object.assign({},e),t.wertung=Object.assign({noteD:0,noteE:0,endnote:0},t.itemOriginal.wertung),t.ionViewWillEnter()}))},e.prototype.ensureInitialValues=function(e){return Object.assign(this.wertung,e)},e.prototype.saveClose=function(e){var t=this;this.waiting=!0,this.backendService.updateWertung(this.durchgang,this.step,this.geraetId,this.ensureInitialValues(e.value)).subscribe((function(e){t.updateUI(e),t.navCtrl.pop()}),(function(e){t.updateUI(t.itemOriginal),console.log(e)}))},e.prototype.save=function(e){var t=this;this.waiting=!0,this.backendService.updateWertung(this.durchgang,this.step,this.geraetId,this.ensureInitialValues(e.value)).subscribe((function(e){t.updateUI(e)}),(function(e){t.updateUI(t.itemOriginal),console.log(e)}))},e.prototype.saveNext=function(e){var t=this;this.waiting=!0,this.backendService.updateWertung(this.durchgang,this.step,this.geraetId,this.ensureInitialValues(e.value)).subscribe((function(n){t.waiting=!1;var i=t.backendService.wertungen.findIndex((function(e){return e.wertung.id===n.wertung.id}));i<0&&console.log("unexpected wertung - id matches not with current wertung: "+n.wertung.id);var r=i+1;if(i<0)r=0;else if(i>=t.backendService.wertungen.length-1){if(0===t.backendService.wertungen.filter((function(e){return void 0===e.wertung.endnote})).length)return t.navCtrl.pop(),void t.toastSuggestCompletnessCheck();r=t.backendService.wertungen.findIndex((function(e){return void 0===e.wertung.endnote})),t.toastMissingResult(e,t.backendService.wertungen[r].vorname+" "+t.backendService.wertungen[r].name)}e.resetForm(),t.updateUI(t.backendService.wertungen[r])}),(function(e){t.updateUI(t.itemOriginal),console.log(e)}))},e.prototype.nextEmptyOrFinish=function(e){var t=this,n=this.backendService.wertungen.findIndex((function(e){return e.wertung.id===t.wertung.id}));if(0===this.backendService.wertungen.filter((function(e,t){return t>n&&void 0===e.wertung.endnote})).length)return this.navCtrl.pop(),void this.toastSuggestCompletnessCheck();var i=this.backendService.wertungen.findIndex((function(e,t){return t>n&&void 0===e.wertung.endnote}));this.toastMissingResult(e,this.backendService.wertungen[i].vorname+" "+this.backendService.wertungen[i].name),e.resetForm(),this.updateUI(this.backendService.wertungen[i])},e.prototype.geraetName=function(){var e=this;return this.backendService.geraete?this.backendService.geraete.find((function(t){return t.id===e.geraetId})).name:""},e.prototype.toastSuggestCompletnessCheck=function(){return Object(a.b)(this,void 0,void 0,(function(){return Object(a.d)(this,(function(e){switch(e.label){case 0:return[4,this.toastController.create({header:"Qualit\xe4tskontrolle",message:"Es sind jetzt alle Resultate erfasst. Bitte ZU ZWEIT die Resultate pr\xfcfen und abschliessen!",animated:!0,position:"middle",buttons:[{text:"OK, gelesen.",icon:"checkmark-circle",role:"cancel",handler:function(){console.log("OK, gelesen. clicked")}}]})];case 1:return e.sent().present(),[2]}}))}))},e.prototype.toastMissingResult=function(e,t){return Object(a.b)(this,void 0,void 0,(function(){var n=this;return Object(a.d)(this,(function(i){switch(i.label){case 0:return[4,this.alertCtrl.create({header:"Achtung",subHeader:"Fehlendes Resultat!",message:"Nach der Erfassung der letzten Wertung in dieser Riege scheint es noch leere Wertungen zu geben. Bitte pr\xfcfen, ob "+t.toUpperCase()+" geturnt hat.",buttons:[{text:"Nicht geturnt",role:"edit",handler:function(){n.nextEmptyOrFinish(e)}},{text:"Korrigieren",role:"cancel",handler:function(){console.log("Korrigieren clicked")}}]})];case 1:return i.sent().present(),[2]}}))}))},e.\u0275fac=function(t){return new(t||e)(d.Hb(s.N),d.Hb(o.a),d.Hb(l.b),d.Hb(s.b),d.Hb(s.S),d.Hb(u.a),d.Hb(c.a),d.Hb(s.P),d.Hb(d.z))},e.\u0275cmp=d.Bb({type:e,selectors:[["app-wertung-editor"]],viewQuery:function(e,t){var n;1&e&&(d.pc(b,!0),d.pc(g,!0),d.pc(h,!0)),2&e&&(d.cc(n=d.Tb())&&(t.form=n.first),d.cc(n=d.Tb())&&(t.enote=n.first),d.cc(n=d.Tb())&&(t.dnote=n.first))},decls:33,vars:13,consts:[["slot","start"],["defaultHref","/"],["slot","end"],[1,"athlet"],[1,"riege"],[3,"ngSubmit","keyup.enter"],["wertungsform","ngForm"],[3,"hidden"],["autofocus","","type","number","name","noteD",3,"readonly","disabled","ngModel"],["dnote",""],["autofocus","","type","number","name","noteE","step","0.01","min","0","max","100","required","",3,"readonly","disabled","ngModel"],["enote",""],["readonly","","name","endnote",3,"ngModel"],["endnote",""],["size","large","expand","block","type","submit","color","success",3,"disabled",4,"ngIf"],["size","large","expand","block","color","secondary",3,"disabled","click",4,"ngIf"],["size","large","expand","block","type","submit","color","success",3,"disabled"],["btnSaveNext",""],["slot","start","name","arrow-dropright-circle"],["size","large","expand","block","color","secondary",3,"disabled","click"],["slot","start","name","checkmark-circle"]],template:function(e,t){if(1&e){var n=d.Lb();d.Kb(0,"ion-header"),d.Kb(1,"ion-toolbar"),d.Kb(2,"ion-buttons",0),d.Ib(3,"ion-back-button",1),d.Jb(),d.Kb(4,"ion-title"),d.kc(5),d.Jb(),d.Kb(6,"ion-note",2),d.Kb(7,"div",3),d.kc(8),d.Jb(),d.Kb(9,"div",4),d.kc(10),d.Jb(),d.Jb(),d.Jb(),d.Jb(),d.Kb(11,"ion-content"),d.Kb(12,"form",5,6),d.Sb("ngSubmit",(function(){d.ec(n);var e=d.dc(13);return t.saveNext(e)}))("keyup.enter",(function(){d.ec(n);var e=d.dc(13);return t.saveNext(e)})),d.Kb(14,"ion-list"),d.Kb(15,"ion-item",7),d.Kb(16,"ion-label"),d.kc(17,"D-Note"),d.Jb(),d.Ib(18,"ion-input",8,9),d.Jb(),d.Kb(20,"ion-item"),d.Kb(21,"ion-label"),d.kc(22,"E-Note"),d.Jb(),d.Ib(23,"ion-input",10,11),d.Jb(),d.Kb(25,"ion-item"),d.Kb(26,"ion-label"),d.kc(27,"Endnote"),d.Jb(),d.Ib(28,"ion-input",12,13),d.Jb(),d.Jb(),d.Kb(30,"ion-list"),d.jc(31,p,4,1,"ion-button",14),d.jc(32,f,3,1,"ion-button",15),d.Jb(),d.Jb(),d.Jb()}2&e&&(d.xb(5),d.lc(t.geraetName()),d.xb(3),d.lc(t.item.vorname+" "+t.item.name),d.xb(2),d.lc(t.wertung.riege),d.xb(5),d.ac("hidden",!t.isDNoteUsed),d.xb(3),d.ac("readonly",!t.editable())("disabled",t.waiting)("ngModel",t.wertung.noteD),d.xb(5),d.ac("readonly",!t.editable())("disabled",t.waiting)("ngModel",t.wertung.noteE),d.xb(5),d.ac("ngModel",t.wertung.endnote),d.xb(3),d.ac("ngIf",t.editable()),d.xb(1),d.ac("ngIf",t.editable()))},directives:[s.n,s.J,s.h,s.e,s.f,s.I,s.z,s.k,r.j,r.e,r.f,s.w,s.q,s.v,s.p,s.O,r.d,r.g,r.i,s.R,i.m,s.g,s.o],styles:[".riege[_ngcontent-%COMP%]{font-size:small;color:var(--ion-color-medium)}.athlet[_ngcontent-%COMP%], .riege[_ngcontent-%COMP%]{padding-right:16px}.athlet[_ngcontent-%COMP%]{font-size:larger;font-weight:bolder;color:var(--ion-color-primary)}"]}),e}()}],w=function(){function e(){}return e.\u0275mod=d.Fb({type:e}),e.\u0275inj=d.Eb({factory:function(t){return new(t||e)},providers:[u.a],imports:[[i.c,r.a,s.K,o.i.forChild(v)]]}),e}()}}]);