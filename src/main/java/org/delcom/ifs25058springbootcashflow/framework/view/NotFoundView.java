package org.delcom.ifs25058springbootcashflow.framework.view;

import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.page.Meta;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.ErrorParameter;
import com.vaadin.flow.router.HasErrorParameter;
import com.vaadin.flow.router.NotFoundException;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.RouterLink;

/**
 * Halaman 404 kustom: punya judul, heading utama, dan landmark <main>
 * supaya audit aksesibilitas tidak menemukan dokumen kosong.
 */
@PageTitle("Halaman Tidak Ditemukan")
@Meta(name = "description", content = "Halaman yang Anda cari tidak ditemukan di aplikasi Cashflow.")
public class NotFoundView extends VerticalLayout implements HasErrorParameter<NotFoundException> {

    public NotFoundView() {
        setSizeFull();
        setPadding(false);
        getStyle().set("background", "#F3F4F6");

        H1 title = new H1("Halaman Tidak Ditemukan");
        title.getStyle().set("margin", "0").set("color", "#111827");

        Paragraph message = new Paragraph("Alamat yang kamu buka tidak tersedia. Periksa kembali tautannya.");
        message.getStyle().set("margin", "0").set("color", "#4B5563");

        RouterLink home = new RouterLink("Kembali ke beranda", FinanceView.class);
        home.getStyle().set("color", "#1D4ED8").set("cursor", "pointer");

        Main main = new Main();
        main.setSizeFull();
        main.getStyle()
                .set("display", "flex")
                .set("flex-direction", "column")
                .set("align-items", "center")
                .set("justify-content", "center")
                .set("gap", "0.75rem")
                .set("text-align", "center")
                .set("box-sizing", "border-box");
        main.add(title, message, home);

        add(main);
    }

    @Override
    public int setErrorParameter(BeforeEnterEvent event, ErrorParameter<NotFoundException> parameter) {
        // @PageTitle sudah menangani sebagian besar kasus; ini pengaman.
        event.getUI().getPage().setTitle("Halaman Tidak Ditemukan");
        return 404;
    }
}
